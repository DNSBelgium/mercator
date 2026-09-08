package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.common.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatabaseItemSource}: it leases {@code PENDING} {@code crawl_tasks}
 * rows for <b>its own module only</b>, atomically (no double-claim under concurrency), and
 * stamps the reservation columns / increments {@code attempts} on a fresh claim.
 *
 * <p>Expired-lease recovery is <em>not</em> covered here — that belongs to the lease reaper;
 * this source only ever claims {@code PENDING} rows.
 */
@Slf4j
@Testcontainers
class DatabaseItemSourceTest {

    private static final String INSTANCE_ID = "test-host";

    @SuppressWarnings("deprecation")
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    private JdbcClient jdbcClient;

    @BeforeAll
    static void createSchema() {
        newJdbcClient().sql("""
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
                """).update();
    }

    @BeforeEach
    void setUp() {
        this.jdbcClient = newJdbcClient();
        jdbcClient.sql("truncate table crawl_tasks").update();
    }

    private DatabaseItemSource source(int fetchSize) {
        PipelineProperties.Queue queue = new PipelineProperties.Queue();
        queue.setFetchSize(fetchSize);
        queue.setInstanceId(INSTANCE_ID);
        return new DatabaseItemSource("web", jdbcClient, queue);
    }

    @SuppressWarnings("SameParameterValue")
    private DatabaseItemSource source(String module, int fetchSize, int maxItemsPerPass) {
        PipelineProperties.Queue queue = new PipelineProperties.Queue();
        queue.setFetchSize(fetchSize);
        queue.setMaxItemsPerPass(maxItemsPerPass);
        queue.setInstanceId(INSTANCE_ID);
        return new DatabaseItemSource(module, jdbcClient, queue);
    }

    @Test
    void freshClaim_reservesBatch_andStampsColumns() {
        seed("web", 8);

        List<VisitRequest> leased = source(5).getItems();

        // Exactly fetchSize rows come back, all distinct.
        assertThat(leased).hasSize(5);
        assertThat(leased).extracting(VisitRequest::getVisitId).doesNotHaveDuplicates();

        // The 5 claimed rows are RESERVED with attempts=1 and our reservation stamped;
        // the remaining 3 stay PENDING and untouched.
        assertThat(countWhere("status = 'RESERVED' and attempts = 1 and reserved_by = '" + INSTANCE_ID
                + "' and reservation_id is not null and reserved_timestamp is not null")).isEqualTo(5);
        assertThat(countWhere("status = 'PENDING' and attempts = 0")).isEqualTo(3);
    }

    @Test
    void claim_isModuleScoped() {
        seed("web", 3);
        seed("dns", 3);

        List<VisitRequest> webItems = source(100).getItems();

        // Only web rows are leased; dns rows are left untouched.
        assertThat(webItems).hasSize(3);
        assertThat(countWhere("crawler_module = 'web' and status = 'RESERVED'")).isEqualTo(3);
        assertThat(countWhere("crawler_module = 'dns' and status = 'PENDING'")).isEqualTo(3);
    }

    @Test
    void emptyQueue_returnsNoItems() {
        assertThat(source(5).getItems()).isEmpty();
    }

    @Test
    void pass_isBoundedByMaxItemsPerPass_andClampsTheLastClaim() {
        // 10 PENDING rows, fetchSize 4, budget 6 → polls of 4 then a clamped 2, then done.
        seed("web", 10);
        DatabaseItemSource source = source("web", 4, 6);

        List<VisitRequest> first = source.getItems();   // min(4, 6-0) = 4
        assertThat(first).hasSize(4);
        assertThat(source.isDone()).isFalse();

        List<VisitRequest> second = source.getItems();  // min(4, 6-4) = 2 (clamped)
        assertThat(second).hasSize(2);

        // Budget reached → the pass is done and nothing more is leased.
        assertThat(source.isDone()).isTrue();
        assertThat(countWhere("status = 'RESERVED'")).isEqualTo(6);
        assertThat(countWhere("status = 'PENDING'")).isEqualTo(4);
    }

    @Test
    void pass_endsWhenQueueIsDrained_beforeReachingBudget() {
        // Only 3 rows but a budget of 100 → one full poll, then an empty poll marks it done.
        seed("web", 3);
        try (DatabaseItemSource source = source("web", 5, 100)) {

            assertThat(source.getItems()).hasSize(3);
            assertThat(source.isDone()).isFalse();

            // Next poll finds nothing → drained → done.
            assertThat(source.getItems()).isEmpty();
            assertThat(source.isDone()).isTrue();
        }
    }

    @Test
    void doesNotSleepBetweenPolls() {
        assertThat(source(5).sleepBetweenPolls()).isFalse();
    }

    @Test
    void concurrentClaims_neverDoubleClaimTheSameRow() throws Exception {
        seed("web", 10);

        DatabaseItemSource a = source(5);
        DatabaseItemSource b = source(5);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<List<VisitRequest>> claimA = a::getItems;
            Callable<List<VisitRequest>> claimB = b::getItems;
            Future<List<VisitRequest>> fa = pool.submit(claimA);
            Future<List<VisitRequest>> fb = pool.submit(claimB);

            Set<String> idsA = fa.get().stream().map(VisitRequest::getVisitId).collect(Collectors.toSet());
            Set<String> idsB = fb.get().stream().map(VisitRequest::getVisitId).collect(Collectors.toSet());

            // The key correctness property: the two claimers never lease the same row.
            // (One may legitimately claim 0 rows under the block-then-recheck race.)
            assertThat(Collections.disjoint(idsA, idsB))
                    .as("claimers %s and %s must be disjoint", idsA, idsB)
                    .isTrue();
            // And no row is leased twice in the table.
            assertThat(countWhere("status = 'RESERVED'")).isEqualTo(idsA.size() + idsB.size());
        }
    }

    // --- helpers -------------------------------------------------------------------------

    private void seed(String module, int rows) {
        for (int i = 1; i <= rows; i++) {
            jdbcClient.sql("insert into crawl_tasks (visit_id, domain_name, crawler_module, status, attempts) "
                            + "values (:visitId, :domain, :module, 'PENDING', 0)")
                    .param("visitId", module + "-" + i)
                    .param("domain", "d" + i + ".example")
                    .param("module", module)
                    .update();
        }
    }

    private long countWhere(String predicate) {
        return jdbcClient.sql("select count(*) from crawl_tasks where " + predicate)
                .query(Long.class)
                .single();
    }

    private static JdbcClient newJdbcClient() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        return JdbcClient.create(new JdbcTemplate(ds));
    }
}
