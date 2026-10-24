package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.CRAWL_TASKS_DDL;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.countCrawlTasks;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.postgresJdbcClient;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.seedPendingTasks;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LeaseReaper}: expired {@code RESERVED} leases are recycled to {@code PENDING}
 * (or dead-lettered to {@code FAILED} once {@code max-attempts} is reached); fresh leases and
 * {@code DONE} rows are never touched.
 */
@SuppressWarnings({"SqlResolve", "resource"})
@Testcontainers
class LeaseReaperTest {

    private static final long LEASE_SECONDS = 600;
    private static final int MAX_ATTEMPTS = 5;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private JdbcClient jdbcClient;
    private LeaseReaper reaper;

    @BeforeAll
    static void createSchema() {
        postgresJdbcClient(POSTGRES).sql(CRAWL_TASKS_DDL).update();
    }

    @BeforeEach
    void setUp() {
        jdbcClient = postgresJdbcClient(POSTGRES);
        jdbcClient.sql("truncate table crawl_tasks").update();
        reaper = new LeaseReaper(jdbcClient, properties());
    }

    private static PipelineProperties properties() {
        PipelineProperties properties = new PipelineProperties();
        properties.getQueue().setLeaseDuration(Duration.ofSeconds(LEASE_SECONDS));
        properties.getQueue().setMaxAttempts(MAX_ATTEMPTS);
        return properties;
    }

    @Test
    void recyclesExpiredLease_clearsReservation_keepsAttempts() {
        insertTask("1", "web", "RESERVED", LEASE_SECONDS * 2, 1);

        LeaseReaper.Result result = reaper.reap();

        assertThat(result).isEqualTo(new LeaseReaper.Result(1, 0));
        var row = jdbcClient.sql("select status, reservation_id, reserved_by, reserved_at, attempts from crawl_tasks")
                .query().singleRow();
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("reservation_id")).isNull();
        assertThat(row.get("reserved_by")).isNull();
        assertThat(row.get("reserved_at")).isNull();
        assertThat(row.get("attempts")).as("attempts preserved across recycle").isEqualTo(1);
    }

    @Test
    void ignoresFreshLease() {
        insertTask("1", "web", "RESERVED", 5, 1);

        LeaseReaper.Result result = reaper.reap();

        assertThat(result).isEqualTo(new LeaseReaper.Result(0, 0));
        assertThat(count("status = 'RESERVED' and reservation_id = 'OLD-TOKEN'"))
                .as("a lease that has not expired is left untouched")
                .isEqualTo(1);
    }

    @Test
    void deadLettersAfterMaxAttempts_andRecyclesJustBelow() {
        insertTask("at-max", "web", "RESERVED", LEASE_SECONDS * 2, MAX_ATTEMPTS);
        insertTask("below-max", "web", "RESERVED", LEASE_SECONDS * 2, MAX_ATTEMPTS - 1);

        LeaseReaper.Result result = reaper.reap();

        assertThat(result).isEqualTo(new LeaseReaper.Result(1, 1));
        assertThat(count("visit_id = 'at-max' and status = 'FAILED'"))
                .as("expired lease at maxAttempts is dead-lettered, not recycled")
                .isEqualTo(1);
        assertThat(count("visit_id = 'below-max' and status = 'PENDING'")).isEqualTo(1);
    }

    @Test
    void leavesDoneRowsUntouched_howeverOldTheirLeaseLooks() {
        insertTask("1", "web", "DONE", LEASE_SECONDS * 2, MAX_ATTEMPTS);

        LeaseReaper.Result result = reaper.reap();

        assertThat(result).isEqualTo(new LeaseReaper.Result(0, 0));
        var row = jdbcClient.sql("select status, reservation_id, reserved_by, reserved_at, attempts from crawl_tasks")
                .query().singleRow();
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("reservation_id")).isEqualTo("OLD-TOKEN");
        assertThat(row.get("reserved_by")).isEqualTo("host-DEAD");
        assertThat(row.get("reserved_at")).isNotNull();
        assertThat(row.get("attempts")).isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void leavesPendingAndFailedRowsUntouched() {
        insertTask("pending", "web", "PENDING", 0, 0);
        insertTask("failed", "web", "FAILED", LEASE_SECONDS * 2, MAX_ATTEMPTS);

        assertThat(reaper.reap()).isEqualTo(new LeaseReaper.Result(0, 0));
        assertThat(count("visit_id = 'pending' and status = 'PENDING'")).isEqualTo(1);
        assertThat(count("visit_id = 'failed' and status = 'FAILED'")).isEqualTo(1);
    }

    @Test
    void coversAllModules() {
        insertTask("w", "web", "RESERVED", LEASE_SECONDS * 2, 1);
        insertTask("d", "dns", "RESERVED", LEASE_SECONDS * 2, 1);
        insertTask("s", "smtp", "RESERVED", LEASE_SECONDS * 2, 1);

        assertThat(reaper.reap()).isEqualTo(new LeaseReaper.Result(3, 0));
        assertThat(count("status = 'PENDING'")).isEqualTo(3);
    }

    @Test
    void runningTwice_isANoOpTheSecondTime() {
        insertTask("1", "web", "RESERVED", LEASE_SECONDS * 2, 1);

        assertThat(reaper.reap()).isEqualTo(new LeaseReaper.Result(1, 0));
        assertThat(reaper.reap()).isEqualTo(new LeaseReaper.Result(0, 0));
    }

    @Test
    void twoConcurrentReapers_doNotDoubleProcessRows() throws Exception {
        int rows = 300;
        jdbcClient.sql("""
                insert into crawl_tasks (visit_id, domain_name, crawler_module, status,
                                         reserved_by, reservation_id, reserved_at, attempts)
                select 'v' || g, 'd' || g || '.example', 'web', 'RESERVED',
                       'host-DEAD', 'OLD-TOKEN', now() - interval '%d seconds', 1
                from generate_series(1, %d) g
                """.formatted(LEASE_SECONDS * 2, rows)).update();

        CyclicBarrier startTogether = new CyclicBarrier(2);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<LeaseReaper.Result>> futures = List.of(
                    pool.submit(() -> {
                        startTogether.await(10, TimeUnit.SECONDS);
                        return new LeaseReaper(jdbcClient, properties()).reap();
                    }),
                    pool.submit(() -> {
                        startTogether.await(10, TimeUnit.SECONDS);
                        return new LeaseReaper(jdbcClient, properties()).reap();
                    }));

            int recycled = 0;
            for (Future<LeaseReaper.Result> future : futures) {
                recycled += future.get(30, TimeUnit.SECONDS).recycled();
            }
            assertThat(recycled).as("every row is recycled exactly once across both reapers").isEqualTo(rows);
        }
        assertThat(count("status = 'PENDING'")).isEqualTo(rows);
    }

    @Test
    void recycledRowCanBeClaimedAgain_byANormalPoll() {
        seedPendingTasks(jdbcClient, "web", 1);
        PipelineProperties.Queue queue = new PipelineProperties.Queue();
        queue.setInstanceId("host-A");

        List<VisitRequest> first = new DatabaseItemSource("web", jdbcClient, queue).getItems();
        assertThat(first).hasSize(1);
        jdbcClient.sql("update crawl_tasks set reserved_at = now() - interval '%d seconds'"
                .formatted(LEASE_SECONDS * 2)).update();

        assertThat(reaper.reap()).isEqualTo(new LeaseReaper.Result(1, 0));

        List<VisitRequest> second = new DatabaseItemSource("web", jdbcClient, queue).getItems();
        assertThat(second).extracting(VisitRequest::getVisitId).containsExactly("web-1");
        assertThat(count("status = 'RESERVED' and attempts = 2")).as("second claim counts as attempt 2").isEqualTo(1);
    }

    @Test
    void databaseFailure_isSwallowed_soTheScheduleKeepsRunning() {
        DriverManagerDataSource unreachable = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:1/nothing", "nobody", "nothing");
        unreachable.setDriverClassName("org.postgresql.Driver");

        LeaseReaper broken = new LeaseReaper(JdbcClient.create(unreachable), properties());

        assertThat(broken.reap()).isEqualTo(new LeaseReaper.Result(0, 0));
    }

    // --- helpers -------------------------------------------------------------------------

    /** Inserts one task whose lease was taken {@code ageSeconds} ago. */
    private void insertTask(String visitId, String module, String status, long ageSeconds, int attempts) {
        jdbcClient.sql("""
                        insert into crawl_tasks (visit_id, domain_name, crawler_module, status,
                                                 reserved_by, reservation_id, reserved_at, attempts)
                        values (:visitId, :domain, :module, :status, 'host-DEAD', 'OLD-TOKEN',
                                now() - interval '%d seconds', :attempts)
                        """.formatted(ageSeconds))
                .param("visitId", visitId)
                .param("domain", visitId + ".example")
                .param("module", module)
                .param("status", status)
                .param("attempts", attempts)
                .update();
    }

    private long count(String predicate) {
        return countCrawlTasks(jdbcClient, predicate);
    }
}

