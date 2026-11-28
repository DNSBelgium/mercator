package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.common.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Stateful, module-scoped {@link ItemSource} backed by the Postgres {@code crawl_tasks}
 * work queue. One instance exists per crawler module (web, dns, smtp, …); it only ever
 * sees rows whose {@code crawler_module} matches {@link #crawlerModule}.
 *
 * <p>Each {@link #getItems()} poll atomically <b>leases</b> a bounded batch of
 * {@code PENDING} rows with a single {@code UPDATE … RETURNING} statement:
 * <ol>
 *   <li>a sub-select picks up to {@code fetchSize} {@code PENDING} rows by {@code ctid}
 *       using {@code FOR UPDATE SKIP LOCKED}, so concurrent claimers never block on, or double-lease,
 *       the same rows (and duplicate {@code visit_id}s cannot make a claim overshoot its LIMIT);</li>
 *   <li>the {@code UPDATE} stamps a fresh per-poll {@code reservation_id} (UUID, kept as an audit trail),
 *       flips the rows to {@code RESERVED}, increments {@code attempts} and returns the leased rows —
 *       no separate read-back round trip.</li>
 * </ol>
 * The lookup is served by the partial index {@code idx_crawl_tasks_pending}
 * ({@code WHERE status = 'PENDING'}), so an empty queue is answered in milliseconds regardless of
 * how many {@code DONE} rows the table holds.
 *
 * <p><b>Recovery is out-of-band:</b> this source never reasons about expired {@code RESERVED}
 * rows — the scheduled {@link LeaseReaper} recycles them back to {@code PENDING}.
 * A crash after leasing but before the Parquet roll-up, or items skipped by a graceful stop,
 * simply leave the lease to expire.
 *
 * <p><b>Task lifecycle:</b>
 * <pre>
 *            claim (getItems)                     ack (acknowledge, after the Parquet roll-up)
 *  PENDING ───────────────────▶ RESERVED ─────────────────────────────────────▶ DONE
 *     ▲                            │            (finished_at = now())
 *     └─ reaper: lease expired, ───┤
 *        attempts &lt; max           └─ reaper: lease expired, attempts &gt;= max ──▶ FAILED
 * </pre>
 *
 * <p><b>Ack.</b> Once a batch of results is durably in Parquet, the writer calls
 * {@link #acknowledge(Collection)}, which sets {@code status = 'DONE'} and
 * {@code finished_at = now()} for exactly those {@code visit_id}s (scoped to this
 * module). Only results that reached Parquet are acked: rows whose processor returned
 * {@code null}/threw, or whose roll-up failed, stay {@code RESERVED} and are left to the
 * reaper. The ack is guarded by {@code status <> 'DONE'} rather than
 * {@code status = 'RESERVED'}: Parquet is the source of truth, so a late ack still wins over
 * a lease race (row recycled, re-leased or dead-lettered meanwhile) instead of causing a
 * pointless re-crawl. The reservation columns are left untouched as an audit trail, so
 * {@code finished_at - reserved_at} is the per-task latency. The ack touches no
 * polling state, so it is safe on the writer thread and after {@link #close()}.
 *
 * <p><b>At-least-once.</b> Parquet and Postgres cannot share a transaction: a crash between
 * "Parquet written" and "ack committed" leaves the row {@code RESERVED}; after the lease
 * expires it is crawled again and a second Parquet row with the same {@code visit_id}
 * appears. Consumers can de-duplicate on {@code visit_id}. Also, a result is acked only when
 * its batch ({@code pipeline.batch-size}) is complete or the pass is flushed, so the
 * {@code lease-duration} must exceed the worst-case time from lease to ack.
 *
 * <p><b>Bounded pass.</b> Rather than polling forever, a source runs a
 * <em>bounded pass</em>: {@link #getItems()} leases in {@code fetchSize} chunks until it has
 * produced {@code maxItemsPerPass} rows <em>or</em> a poll finds the queue empty. Each
 * claim's {@code LIMIT} is clamped to {@code min(fetchSize, maxItemsPerPass - produced)} so a
 * pass never overshoots its budget. {@link #isDone()} then becomes {@code true} and the
 * producer drains and shuts the pass down (poison pill → writer flush → ack). The sequential
 * {@code QueueModuleRunner} advances to the next module and, when a whole cycle finds no
 * work, sleeps — so this source never sleeps between polls ({@link #sleepBetweenPolls()} is
 * {@code false}).
 */
@SuppressWarnings("SqlResolve")
@Slf4j
public class DatabaseItemSource implements ItemSource<VisitRequest> {

    /**
     * Closes tasks whose result is durably stored. {@code status <> 'DONE'} (instead of
     * {@code = 'RESERVED'}) keeps the first {@code finished_at} on retries/duplicates and
     * still closes rows a lease race recycled or dead-lettered in the meantime.
     */
    private static final String ACK_SQL = """
            update crawl_tasks
            set    status        = 'DONE',
                   finished_at   = now()
            where  crawler_module = :module
              and  status <> 'DONE'
              and  visit_id in (:visitIds)
            """;

    /** Attempts at the claim/ack UPDATE before giving up (covers transient serialization/deadlock errors). */
    private static final int MAX_CLAIM_RETRIES = 5;

    /** Max ids per ack UPDATE: far below Postgres' 32 767 bind-parameter limit, even for a big batch-size. */
    private static final int ACK_CHUNK_SIZE = 1000;

    private final String crawlerModule;
    private final JdbcClient jdbcClient;
    private final String instanceId;
    private final int fetchSize;
    private final int maxItemsPerPass;

    /** Claim UPDATE with a {@code %d} placeholder for this poll's clamped {@code LIMIT}. */
    private final String claimSqlTemplate;

    private volatile boolean running = true;

    /** Rows leased so far in the current pass (single producer thread — no synchronization needed). */
    private int produced = 0;

    /** Set once a claim returns no rows: the queue is drained, so the pass is done. */
    private boolean drained = false;

    public DatabaseItemSource(String crawlerModule,
                              JdbcClient queueJdbcClient,
                              PipelineProperties.Queue queueProperties) {
        this.crawlerModule = crawlerModule;
        this.jdbcClient = queueJdbcClient;
        this.instanceId = queueProperties.getInstanceId();
        this.fetchSize = queueProperties.getFetchSize();
        this.maxItemsPerPass = queueProperties.getMaxItemsPerPass();
        this.claimSqlTemplate = """
                update crawl_tasks
                set    status             = 'RESERVED',
                       reserved_by        = :instanceId,
                       reservation_id     = :token,
                       reserved_at        = now(),
                       attempts           = attempts + 1
                where  ctid in (
                        select ctid
                        from   crawl_tasks
                        where  crawler_module = :module
                          and  status = 'PENDING'
                        order by visit_id
                        limit %d
                        for update skip locked
                     )
                returning visit_id, domain_name
                """;
        log.info("DatabaseItemSource for module '{}' (fetchSize={}, maxItemsPerPass={}, instanceId={})",
                crawlerModule, fetchSize, maxItemsPerPass, instanceId);
        log.debug("claimSql:\n {}", claimSqlTemplate);
    }

    @Override
    public List<VisitRequest> getItems() {
        // Clamp this poll's LIMIT so the pass never leases more than maxItemsPerPass rows.
        int limit = Math.min(fetchSize, maxItemsPerPass - produced);
        log.debug("getItems() with limit={}", limit);
        if (limit <= 0) {
            return List.of();
        }
        String token = UUID.randomUUID().toString();
        List<VisitRequest> items = claimBatch(token, limit);
        log.info("getItems() claimed={} items", items.size());
        if (items.isEmpty()) {
            drained = true;
            return items;
        }
        produced += items.size();
        log.info("[{}] leased {} rows (reservation_id={}, produced={}/{})",
                crawlerModule, items.size(), token, produced, maxItemsPerPass);
        return items;
    }

    /**
     * Marks the given tasks {@code DONE} (see the class Javadoc). Runs in chunks of
     * {@value #ACK_CHUNK_SIZE} ids, each with the same retry/backoff as the claim. The update
     * count is advisory: a WARN is logged when it is lower than the number of ids sent.
     *
     * <p>Touches no instance state, so it is safe on the writer thread and after {@link #close()}.
     *
     * @return the number of rows that were actually updated
     */
    @Override
    public int acknowledge(Collection<String> visitIds) {
        if (visitIds == null || visitIds.isEmpty()) {
            return 0;
        }
        List<String> ids = new ArrayList<>(visitIds);
        int updated = 0;
        for (int from = 0; from < ids.size(); from += ACK_CHUNK_SIZE) {
            List<String> chunk = ids.subList(from, Math.min(from + ACK_CHUNK_SIZE, ids.size()));
            updated += withRetries("ack", () -> jdbcClient.sql(ACK_SQL)
                    .param("module", crawlerModule)
                    .param("visitIds", chunk)
                    .update()).intValue();
        }
        if (updated < ids.size()) {
            log.warn("[{}] acknowledged only {}/{} task(s) as DONE (the others were unknown or already DONE)",
                    crawlerModule, updated, ids.size());
        } else {
            log.info("[{}] acknowledged {}/{} task(s) as DONE", crawlerModule, updated, ids.size());
        }
        return updated;
    }

    /**
     * Runs the claim {@code UPDATE … RETURNING}, retrying a few times on {@link DataAccessException}
     * (e.g. a transient deadlock or serialization failure).
     *
     * @param limit this poll's clamped {@code LIMIT} (a computed int, so safe to inline)
     * @return the rows leased by this poll
     */
    private List<VisitRequest> claimBatch(String token, int limit) {
        String claimSql = claimSqlTemplate.formatted(limit);
        return withRetries("claim", () -> jdbcClient.sql(claimSql)
                .param("module", crawlerModule)
                .param("token", token)
                .param("instanceId", instanceId)
                .query((row, _) -> {
                    VisitRequest visitRequest = new VisitRequest();
                    visitRequest.setVisitId(row.getString("visit_id"));
                    visitRequest.setDomainName(row.getString("domain_name"));
                    return visitRequest;
                })
                .list());
    }

    /**
     * Runs {@code action}, retrying up to {@value #MAX_CLAIM_RETRIES} attempts on
     * {@link DataAccessException} with a short jittered backoff.
     *
     * @param operation short name used in the log lines ({@code claim}, {@code ack})
     */
    private <T> T withRetries(String operation, Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (DataAccessException e) {
                attempt++;
                if (attempt >= MAX_CLAIM_RETRIES) {
                    log.error("[{}] {} failed after {} attempts", crawlerModule, operation, attempt, e);
                    throw e;
                }
                log.warn("[{}] {} attempt {} failed ({}); retrying", crawlerModule, operation, attempt, e.getMessage());
                backoff(attempt);
            }
        }
    }

    /** Short jittered backoff between claim/ack retries. */
    private void backoff(int attempt) {
        long millis = 25L * attempt + ThreadLocalRandom.current().nextLong(25);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry backoff", ie);
        }
    }

    @Override
    public boolean isDone() {
        return !running || drained || produced >= maxItemsPerPass;
    }

    /**
     * A bounded-pass source never sleeps between polls: an empty poll <em>ends the pass</em>
     * (sets {@code drained}), and idle waiting when the whole queue is empty is the
     * {@code QueueModuleRunner}'s job, not this source's.
     */
    @Override
    public boolean sleepBetweenPolls() {
        return false;
    }

    @Override
    public void close() {
        log.info("Closing DatabaseItemSource for module '{}' (produced {} rows this pass)",
                crawlerModule, produced);
        this.running = false;
    }
}
