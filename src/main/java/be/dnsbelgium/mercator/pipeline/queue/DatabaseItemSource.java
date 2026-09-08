package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.common.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    /** Attempts at the claim UPDATE before giving up (covers DuckDB/GizmoSQL MVCC conflicts). */
    private static final int MAX_CLAIM_RETRIES = 5;

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
                       reserved_timestamp = now(),
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
    }

    @Override
    public List<VisitRequest> getItems() {
        // Clamp this poll's LIMIT so the pass never leases more than maxItemsPerPass rows.
        int limit = Math.min(fetchSize, maxItemsPerPass - produced);
        if (limit <= 0) {
            return List.of();
        }
        String token = UUID.randomUUID().toString();
        int claimed = claimBatch(token, limit);
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
        int attempt = 0;
        while (true) {
            try {
                return jdbcClient.sql(claimSql)
                        .param("module", crawlerModule)
                        .param("token", token)
                        .param("instanceId", instanceId)
                        .update();
            } catch (DataAccessException e) {
                attempt++;
                if (attempt >= MAX_CLAIM_RETRIES) {
                    log.error("[{}] claim failed after {} attempts", crawlerModule, attempt, e);
                    throw e;
                }
                log.warn("[{}] claim attempt {} conflicted ({}); retrying", crawlerModule, attempt, e.getMessage());
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

    /** Short jittered backoff between claim retries. */
    private void backoff(int attempt) {
        long millis = 25L * attempt + ThreadLocalRandom.current().nextLong(25);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during claim backoff", ie);
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
