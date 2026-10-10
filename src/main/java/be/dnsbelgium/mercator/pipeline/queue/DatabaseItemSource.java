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
import java.util.function.IntSupplier;

/**
 * Stateful, module-scoped {@link ItemSource} backed by the Postgres {@code crawl_tasks}
 * work queue. One instance exists per crawler module (web, dns, smtp, …); it only ever
 * sees rows whose {@code crawler_module} matches {@link #crawlerModule}.
 *
 * <p>Each {@link #getItems()} poll atomically <b>leases</b> a bounded batch of
 * {@code PENDING} rows using the portable "claim-token" pattern (no {@code FOR UPDATE SKIP
 * LOCKED}, no {@code UPDATE … RETURNING}), proven correct by {@code ReservationBlockThenRecheckTest}:
 * <ol>
 *   <li>an {@code UPDATE} stamps a fresh per-poll {@code reservation_id} (UUID) onto up to
 *       {@code fetchSize} {@code PENDING} rows, flipping them to {@code RESERVED} and
 *       incrementing {@code attempts};</li>
 *   <li>a follow-up {@code SELECT} reads back exactly the rows carrying that token.</li>
 * </ol>
 *
 * <p><b>Recovery is out-of-band:</b> this source never reasons about expired {@code RESERVED}
 * rows — a separate scheduled lease reaper recycles them back to {@code PENDING}.
 * A crash after leasing but before the Parquet roll-up simply leaves the lease to expire.
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

    /** Read-back of the batch we just stamped with our claim token. */
    private static final String READ_BACK_SQL = """
            select visit_id, domain_name
            from   crawl_tasks
            where  reservation_id = :token
              and  crawler_module = :module
              and  status = 'RESERVED'
            """;

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

    /** Attempts at the claim/ack UPDATE before giving up (covers DuckDB/GizmoSQL MVCC conflicts). */
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
                where  crawler_module = :module
                  and  status = 'PENDING'
                  and  visit_id in (
                        select visit_id
                        from   crawl_tasks
                        where  crawler_module = :module
                          and  status = 'PENDING'
                        order by visit_id
                        limit %d
                     )
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
        int claimed = claimBatch(token, limit);
        log.info("getItems() claimed={} items", claimed);
        if (claimed == 0) {
            drained = true;
            return List.of();
        }
        List<VisitRequest> items = readClaimedBatch(token);
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
                    .update());
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
     * Runs the claim UPDATE, retrying a few times on {@link DataAccessException}. On
     * DuckDB/GizmoSQL two workers claiming concurrently can hit an optimistic-MVCC
     * write-write conflict (thrown); on Postgres the second claimer blocks instead, so the
     * loop is effectively a no-op there.
     *
     * @param limit this poll's clamped {@code LIMIT} (a computed int, so safe to inline)
     * @return the number of rows reserved by this poll
     */
    private int claimBatch(String token, int limit) {
        String claimSql = claimSqlTemplate.formatted(limit);
        return withRetries("claim", () -> jdbcClient.sql(claimSql)
                .param("module", crawlerModule)
                .param("token", token)
                .param("instanceId", instanceId)
                .update());
    }

    /**
     * Runs {@code action} (an UPDATE returning its row count), retrying up to
     * {@value #MAX_CLAIM_RETRIES} attempts on {@link DataAccessException} with a short jittered backoff.
     *
     * @param operation short name used in the log lines ({@code claim}, {@code ack})
     */
    private int withRetries(String operation, IntSupplier action) {
        int attempt = 0;
        while (true) {
            try {
                return action.getAsInt();
            } catch (DataAccessException e) {
                attempt++;
                if (attempt >= MAX_CLAIM_RETRIES) {
                    log.error("[{}] {} failed after {} attempts", crawlerModule, operation, attempt, e);
                    throw e;
                }
                log.warn("[{}] {} attempt {} conflicted ({}); retrying", crawlerModule, operation, attempt, e.getMessage());
                backoff(attempt);
            }
        }
    }

    private List<VisitRequest> readClaimedBatch(String token) {
        return jdbcClient.sql(READ_BACK_SQL)
                .param("token", token)
                .param("module", crawlerModule)
                .query((row, _) -> {
                    VisitRequest visitRequest = new VisitRequest();
                    visitRequest.setVisitId(row.getString("visit_id"));
                    visitRequest.setDomainName(row.getString("domain_name"));
                    return visitRequest;
                })
                .list();
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
