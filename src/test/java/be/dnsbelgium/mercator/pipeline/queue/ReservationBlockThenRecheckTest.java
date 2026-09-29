package be.dnsbelgium.mercator.pipeline.queue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Note: this test is <b>not</b> a unit test of the {@link DatabaseItemSource} class.
 * This code is obviously AI generated, and it merely proofs a claim made by the agent in its planning phase.
 * <p>
 * Empirical proof for the reservation query in {@code t2-stateful-sources-plan.md} (§4).
 *
 * <p>The plan claims that with the portable "claim-token" lease (no {@code FOR UPDATE SKIP
 * LOCKED}), the {@code status IN ('PENDING','FAILED')} predicate <b>must be repeated in the
 * outer {@code WHERE}</b> — not only in the inner {@code SELECT ... ORDER BY ... LIMIT}
 * subquery — otherwise two concurrent claimers can double-claim the same rows.
 *
 * <p>The reason: the inner subquery is <b>uncorrelated and carries {@code LIMIT}</b>, so
 * PostgreSQL cannot pull it up into a semi-join; it becomes a hashed {@code SubPlan} whose
 * result is computed <b>once</b> and cached. During the READ COMMITTED "block-then-recheck"
 * (EvalPlanQual) that happens when a claimer unblocks after a peer commits, PostgreSQL
 * re-evaluates the <b>outer</b> qualification against the freshly-updated row but does
 * <b>not</b> re-run that cached subquery. So {@code visit_id IN (...)} still matches, and
 * only the repeated outer {@code status} predicate can reject the row.
 *
 * <p>Two tests, identical choreography, differing only by whether the outer {@code WHERE}
 * repeats the status/lease predicate:
 * <ul>
 *   <li>{@link #withoutOuterStatusPredicate_allowsDoubleClaim()} — the second claimer
 *       re-reserves all 5 rows (double-claim).</li>
 *   <li>{@link #withOuterStatusPredicate_preventsDoubleClaim()} — the second claimer
 *       reserves 0 rows.</li>
 * </ul>
 *
 * <p>For the fresh-{@code PENDING} race above, an outer <b>status</b> check alone is enough
 * (a successful claim always moves the row out of {@code ('PENDING','FAILED')}).
 */
@SuppressWarnings("SqlResolve")
@Slf4j
@Testcontainers
class ReservationBlockThenRecheckTest {

    private static final int SEED_ROWS = 5;
    private static final int FETCH_SIZE = 5;
    private static final long LEASE_SECONDS = 600;
    private static final int MAX_ATTEMPTS = 5;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @BeforeAll
    static void createSchema() throws Exception {
        try (Connection c = newConnection(); Statement s = c.createStatement()) {
            s.execute("""
                    create table crawl_tasks (
                        visit_id           text        not null,
                        domain_name        text        not null,
                        crawler_module     text        not null,
                        status             text        not null default 'PENDING',
                        reserved_timestamp timestamptz,
                        reserved_by        text,
                        reservation_id     text,
                        attempts           int         not null default 0,
                        completed_at       timestamptz
                    )
                    """);
        }
    }

    @BeforeEach
    void seed() throws Exception {
        try (Connection c = newConnection(); Statement s = c.createStatement()) {
            s.execute("truncate table crawl_tasks");
            for (int i = 1; i <= SEED_ROWS; i++) {
                s.execute("insert into crawl_tasks (visit_id, domain_name, crawler_module, status, attempts) " +
                        "values ('" + i + "', 'd" + i + ".example', 'web', 'PENDING', 0)");
            }
        }
    }

    @Test
    void withoutOuterStatusPredicate_allowsDoubleClaim() throws Exception {
        int secondClaimerRowCount = runConcurrentClaim(false);

        // The uncorrelated LIMIT subquery result (visit_ids 1..5) is cached and NOT re-run
        // on recheck, and there is no outer status predicate to reject the now-RESERVED
        // rows -> the second claimer re-reserves the very same rows.
        assertThat(secondClaimerRowCount)
                .as("second claimer re-reserves rows already reserved by the first -> double claim")
                .isEqualTo(SEED_ROWS);
        assertThat(reservationIdOfAllRows()).isEqualTo("TOKEN-2");
    }

    @Test
    void withOuterStatusPredicate_preventsDoubleClaim() throws Exception {
        int secondClaimerRowCount = runConcurrentClaim(true);

        // The repeated outer status predicate is re-evaluated against the updated row on
        // recheck; the row is now 'RESERVED', so it fails the qualification and is skipped.
        assertThat(secondClaimerRowCount)
                .as("outer status predicate rejects rows a peer just reserved -> no double claim")
                .isZero();
        assertThat(reservationIdOfAllRows()).isEqualTo("TOKEN-1");
    }

    // ---------------------------------------------------------------------------------------
    // Expired-lease recovery is done OUT-OF-BAND by a scheduled reaper (plan §4.2), keeping
    // the claim query trivial (status = 'PENDING'). These tests validate that reaper.
    // ---------------------------------------------------------------------------------------

    @Test
    void reaper_recyclesExpiredLease() throws Exception {
        seedReservedRow(/* ageSeconds */ LEASE_SECONDS * 2, /* attempts */ 1);

        runReaper();

        // Expired lease with retries left -> back to PENDING, reservation columns cleared.
        try (Connection c = newConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "select status, reservation_id, reserved_by, reserved_timestamp, attempts from crawl_tasks")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("status")).isEqualTo("PENDING");
            assertThat(rs.getString("reservation_id")).isNull();
            assertThat(rs.getString("reserved_by")).isNull();
            assertThat(rs.getTimestamp("reserved_timestamp")).isNull();
            assertThat(rs.getInt("attempts")).as("attempts preserved across recycle").isEqualTo(1);
        }

        // ...and it is now claimable again by a normal poll.
        try (Connection c = newConnection(); Statement s = c.createStatement()) {
            assertThat(s.executeUpdate(claimSql(true, "host-A", "TOKEN-1"))).isEqualTo(1);
        }
    }

    @Test
    void reaper_ignoresFreshLease() throws Exception {
        seedReservedRow(/* ageSeconds */ 5, /* attempts */ 1); // lease still valid

        runReaper();

        try (Connection c = newConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("select status from crawl_tasks")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("status"))
                    .as("a lease that has not expired is left untouched")
                    .isEqualTo("RESERVED");
        }
    }

    @Test
    void reaper_deadLettersAfterMaxAttempts() throws Exception {
        seedReservedRow(/* ageSeconds */ LEASE_SECONDS * 2, /* attempts */ MAX_ATTEMPTS);

        runReaper();

        try (Connection c = newConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("select status from crawl_tasks")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("status"))
                    .as("expired lease past maxAttempts is dead-lettered, not recycled")
                    .isEqualTo("FAILED");
        }
    }

    private int runConcurrentClaim(boolean repeatStatusInOuterWhere) throws Exception {
        return runRace(
                claimSql(repeatStatusInOuterWhere, "host-A", "TOKEN-1"),
                claimSql(repeatStatusInOuterWhere, "host-B", "TOKEN-2")
        );
    }

    /**
     * Choreographs the race so the second claimer deterministically blocks on the first:
     * <ol>
     *   <li>T1 runs its claim UPDATE (locks + updates the contended rows) but does not commit.</li>
     *   <li>T2 runs its claim UPDATE on another connection -> blocks on the row locks.</li>
     *   <li>Once T2 is confirmed blocked, T1 commits, releasing the locks.</li>
     *   <li>T2 unblocks, performs the EvalPlanQual recheck, and commits.</li>
     * </ol>
     *
     * @return the number of rows the second claimer (T2) actually reserved.
     */
    private int runRace(String t1Sql, String t2Sql) throws Exception {
        try (Connection c1 = newConnection(); Connection c2 = newConnection(); ExecutorService executor = Executors.newSingleThreadExecutor()) {
            c1.setAutoCommit(false);
            c2.setAutoCommit(false);

            int t1Count;
            try (Statement s1 = c1.createStatement()) {
                t1Count = s1.executeUpdate(t1Sql);
            }
            assertThat(t1Count).as("first claimer reserves the contended rows").isEqualTo(ReservationBlockThenRecheckTest.SEED_ROWS);

            Future<Integer> t2Future = executor.submit(() -> {
                try (Statement s2 = c2.createStatement()) {
                    return s2.executeUpdate(t2Sql); // blocks until c1 commits
                }
            });

            awaitSecondClaimerBlockedOnLock();

            c1.commit(); // release the row locks -> T2 unblocks and rechecks

            int t2Count = t2Future.get(30, TimeUnit.SECONDS);
            c2.commit();
            return t2Count;
        }
    }

    /**
     * The claim statement from §4 of the plan. Only {@code PENDING} rows are claimable; the
     * only difference between the two variants is whether the {@code status} predicate is
     * repeated in the outer {@code WHERE} (the concurrency guard proved by the tests above).
     */
    private static String claimSql(boolean repeatStatusInOuterWhere, String host, String token) {
        String outerStatusPredicate = repeatStatusInOuterWhere ? "  and status = 'PENDING'\n" : "";

        return """
                update crawl_tasks
                set    status = 'RESERVED',
                       reserved_by = '%s',
                       reservation_id = '%s',
                       reserved_timestamp = now(),
                       attempts = attempts + 1
                where  crawler_module = 'web'
                %s  and visit_id in (
                        select visit_id
                        from   crawl_tasks
                        where  crawler_module = 'web'
                          and  status = 'PENDING'
                        order by visit_id
                        limit %d
                     )
                """.formatted(host, token, outerStatusPredicate, FETCH_SIZE);
    }

    /** Runs the lease reaper (plan §4.2): recycle expired leases, dead-letter exhausted ones. */
    private void runReaper() throws Exception {
        String cutoff = "reserved_timestamp < now() - interval '" + LEASE_SECONDS + " seconds'";
        try (Connection c = newConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    update crawl_tasks
                    set    status = 'PENDING', reservation_id = null, reserved_by = null, reserved_timestamp = null
                    where  status = 'RESERVED' and %s and attempts < %d
                    """.formatted(cutoff, MAX_ATTEMPTS));
            s.executeUpdate("""
                    update crawl_tasks
                    set    status = 'FAILED'
                    where  status = 'RESERVED' and %s and attempts >= %d
                    """.formatted(cutoff, MAX_ATTEMPTS));
        }
    }

    /** Truncates and inserts a single {@code RESERVED} row with the given lease age and attempt count. */
    private void seedReservedRow(long ageSeconds, int attempts) throws Exception {
        try (Connection c = newConnection(); Statement s = c.createStatement()) {
            s.execute("truncate table crawl_tasks");
            s.execute("insert into crawl_tasks " +
                    "(visit_id, domain_name, crawler_module, status, reserved_by, reservation_id, reserved_timestamp, attempts) " +
                    "values ('1', 'd1.example', 'web', 'RESERVED', 'host-DEAD', 'OLD-TOKEN', " +
                    "now() - interval '" + ageSeconds + " seconds', " + attempts + ")");
        }
    }

    /** Polls {@code pg_stat_activity} (from a third connection) until T2's UPDATE is waiting on a lock. */
    private void awaitSecondClaimerBlockedOnLock() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (Connection watcher = newConnection()) {
            while (System.nanoTime() < deadline) {
                try (Statement s = watcher.createStatement();
                     ResultSet rs = s.executeQuery(
                             "select count(*) from pg_stat_activity " +
                                     "where wait_event_type = 'Lock' " +
                                     "and query ilike 'update crawl_tasks%'")) {
                    if (rs.next() && rs.getInt(1) >= 1) {
                        return;
                    }
                }
                TimeUnit.MILLISECONDS.sleep(50);
            }
        }
        throw new IllegalStateException("Second claimer never became blocked on a row lock");
    }

    private String reservationIdOfAllRows() throws Exception {
        try (Connection c = newConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("select distinct reservation_id from crawl_tasks")) {
            assertThat(rs.next()).isTrue();
            String reservationId = rs.getString(1);
            assertThat(rs.next()).as("all rows share a single reservation_id").isFalse();
            return reservationId;
        }
    }

    private static Connection newConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}

