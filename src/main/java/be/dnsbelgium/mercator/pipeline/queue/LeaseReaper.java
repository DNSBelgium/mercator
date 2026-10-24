package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Recovers {@code crawl_tasks} rows that were leased ({@code RESERVED}) but never acked: the
 * worker crashed or was killed, the Parquet roll-up failed, or a graceful stop skipped the
 * item (see {@code PipelineService}). {@link DatabaseItemSource} itself never reasons about
 * expired leases; this scheduled job is the only recovery path.
 *
 * <p>Two single-statement UPDATEs, both only touching {@code RESERVED} rows whose lease
 * ({@code reserved_at}) is older than {@code pipeline.queue.lease-duration}:
 * <ol>
 *   <li><b>recycle</b> rows with {@code attempts < max-attempts} back to {@code PENDING}
 *       (reservation columns cleared, {@code attempts} kept: the claim already counted it);</li>
 *   <li><b>dead-letter</b> rows with {@code attempts >= max-attempts} to {@code FAILED}.</li>
 * </ol>
 * The two predicates are disjoint, so the order does not matter. {@code DONE} rows are never
 * touched.
 *
 * <p>The cutoff is computed by the <b>database clock</b> ({@code now()}), the same clock that
 * stamps {@code reserved_at} on a claim, so application/database clock skew is irrelevant. The
 * lease seconds are a computed integer and are inlined into the SQL (same technique as the
 * claim {@code LIMIT}), which keeps the statements portable across Postgres / DuckDB / GizmoSQL.
 *
 * <p>Safe to run on several instances concurrently and while a pass is running: a row changes
 * status at most once per expiry, a concurrent claim only ever sees {@code PENDING} rows, and a
 * late ack still wins ({@code DatabaseItemSource.acknowledge} accepts any status except
 * {@code DONE}).
 *
 * <p><b>Constraint:</b> {@code lease-duration} must be larger than the worst-case time from
 * lease to ack (a whole pass). Otherwise a row that is still being processed is recycled and
 * crawled a second time (a duplicate, never a loss).
 */
@SuppressWarnings("SqlResolve")
@Slf4j
@Component
@Profile("postgres-queue")
public class LeaseReaper {

    /** What one {@link #reap()} run did. */
    public record Result(int recycled, int deadLettered) {
        static final Result NOTHING = new Result(0, 0);
    }

    private final JdbcClient jdbcClient;
    private final int maxAttempts;
    private final Duration leaseDuration;
    private final String recycleSql;
    private final String deadLetterSql;

    public LeaseReaper(@Qualifier("queueJdbcClient") JdbcClient queueJdbcClient,
                       PipelineProperties properties) {
        this.jdbcClient = queueJdbcClient;
        PipelineProperties.Queue queue = properties.getQueue();
        this.maxAttempts = queue.getMaxAttempts();
        this.leaseDuration = queue.getLeaseDuration();
        // A computed integer (never user text), so it is safe to inline.
        long leaseSeconds = Math.max(1, leaseDuration.toSeconds());
        String expired = "reserved_at < now() - interval '" + leaseSeconds + " seconds'";
        this.recycleSql = """
                update crawl_tasks
                set    status         = 'PENDING',
                       reservation_id = null,
                       reserved_by    = null,
                       reserved_at    = null
                where  status = 'RESERVED'
                  and  %s
                  and  attempts < :maxAttempts
                """.formatted(expired);
        this.deadLetterSql = """
                update crawl_tasks
                set    status = 'FAILED'
                where  status = 'RESERVED'
                  and  %s
                  and  attempts >= :maxAttempts
                """.formatted(expired);
        log.info("LeaseReaper: leaseDuration={}, maxAttempts={}, interval={}",
                leaseDuration, maxAttempts, queue.getReaperInterval());
    }

    /**
     * Recycles / dead-letters expired leases. Scheduled at {@code pipeline.queue.reaper-interval}
     * (fixed delay: runs never overlap, and the first run happens right after start-up so leases
     * that expired while the application was down are recycled immediately); also callable
     * directly (e.g. in tests).
     *
     * <p>A database error is logged and swallowed: the next run simply tries again.
     *
     * @return how many rows were recycled and dead-lettered by this run
     */
    @Scheduled(fixedDelayString = "${pipeline.queue.reaper-interval:1m}")
    public Result reap() {
        try {
            int recycled = jdbcClient.sql(recycleSql).param("maxAttempts", maxAttempts).update();
            int deadLettered = jdbcClient.sql(deadLetterSql).param("maxAttempts", maxAttempts).update();
            if (recycled + deadLettered > 0) {
                log.info("lease reaper: recycled {} expired lease(s), dead-lettered {} (max-attempts={}, lease-duration={})",
                        recycled, deadLettered, maxAttempts, leaseDuration);
            } else {
                log.debug("lease reaper: no expired leases");
            }
            return new Result(recycled, deadLettered);
        } catch (DataAccessException e) {
            log.warn("lease reaper failed; will retry at the next interval", e);
            return Result.NOTHING;
        }
    }
}

