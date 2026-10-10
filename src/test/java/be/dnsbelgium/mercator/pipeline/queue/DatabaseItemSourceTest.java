package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.common.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.CRAWL_TASKS_DDL;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.countCrawlTasks;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.postgresJdbcClient;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.seedPendingTasks;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatabaseItemSource}: it leases {@code PENDING} {@code crawl_tasks}
 * rows for <b>its own module only</b>, atomically (no double-claim under concurrency), and
 * stamps the reservation columns / increments {@code attempts} on a fresh claim. After a
 * result is durably stored, {@link DatabaseItemSource#acknowledge} closes the task
 * ({@code DONE} + {@code finished_timestamp}).
 *
 * <p>Expired-lease recovery is <em>not</em> covered here — that belongs to the lease reaper;
 * this source only ever claims {@code PENDING} rows.
 */
@Slf4j
@Testcontainers
class DatabaseItemSourceTest {

    private static final String INSTANCE_ID = "test-host";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private JdbcClient jdbcClient;

    @BeforeAll
    static void createSchema() {
        postgresJdbcClient(POSTGRES).sql(CRAWL_TASKS_DDL).update();
    }

    @BeforeEach
    void setUp() {
        this.jdbcClient = postgresJdbcClient(POSTGRES);
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

    // --- acknowledge ---------------------------------------------------------------------

    @Test
    void acknowledge_marksLeasedRowsDone_andStampsFinishedTimestamp() {
        seed("web", 5);
        DatabaseItemSource source = source(100);
        List<String> leased = source.getItems().stream().map(VisitRequest::getVisitId).toList();
        List<String> acked = leased.subList(0, 3);
        List<Map<String, Object>> reservationBefore = reservationColumns(acked);
        Instant before = databaseNow();

        int updated = source.acknowledge(acked);

        Instant after = databaseNow();
        assertThat(updated).isEqualTo(3);
        // Exactly the acked rows are DONE, with a finished_timestamp taken from the DB clock.
        assertThat(countWhere("status = 'DONE' and finished_timestamp is not null")).isEqualTo(3);
        assertThat(finishedTimestamps(acked)).hasSize(3).allSatisfy(ts -> assertThat(ts).isBetween(before, after));
        // The other two leased rows stay RESERVED and open.
        assertThat(countWhere("status = 'RESERVED' and finished_timestamp is null")).isEqualTo(2);
        // The reservation columns are an audit trail and are left untouched.
        assertThat(reservationColumns(acked)).isEqualTo(reservationBefore);
    }

    @Test
    void acknowledge_isModuleScoped() {
        insertTask("v1", "web", "RESERVED");
        insertTask("v1", "dns", "RESERVED");

        assertThat(source(5).acknowledge(List.of("v1"))).isEqualTo(1);

        assertThat(countWhere("crawler_module = 'web' and status = 'DONE'")).isEqualTo(1);
        assertThat(countWhere("crawler_module = 'dns' and status = 'RESERVED' and finished_timestamp is null"))
                .isEqualTo(1);
    }

    @Test
    void acknowledge_isIdempotent_andKeepsFirstTimestamp() {
        insertTask("v1", "web", "RESERVED");
        DatabaseItemSource source = source(5);

        assertThat(source.acknowledge(List.of("v1"))).isEqualTo(1);
        List<Instant> first = finishedTimestamps(List.of("v1"));

        assertThat(source.acknowledge(List.of("v1"))).isZero();

        assertThat(finishedTimestamps(List.of("v1"))).isEqualTo(first);
        assertThat(countWhere("status = 'DONE'")).isEqualTo(1);
    }

    @Test
    void acknowledge_ignoresUnknownIds_andEmptyInput() {
        insertTask("v1", "web", "RESERVED");
        DatabaseItemSource source = source(5);

        assertThat(source.acknowledge(List.of("unknown"))).isZero();
        assertThat(source.acknowledge(List.of())).isZero();
        assertThat(source.acknowledge(null)).isZero();

        assertThat(countWhere("status = 'RESERVED' and finished_timestamp is null")).isEqualTo(1);
    }

    @Test
    void acknowledge_takesOverRowsRecycledOrFailed() {
        // Parquet is the source of truth: a late ack wins over a lease race (recycled / dead-lettered / re-leased).
        insertTask("recycled", "web", "PENDING");
        insertTask("failed", "web", "FAILED");
        insertTask("reserved", "web", "RESERVED");

        assertThat(source(5).acknowledge(List.of("recycled", "failed", "reserved"))).isEqualTo(3);

        assertThat(countWhere("status = 'DONE' and finished_timestamp is not null")).isEqualTo(3);
    }

    @Test
    void acknowledge_chunksLargeBatches() {
        int rows = 2_500;
        jdbcClient.sql("""
                insert into crawl_tasks (visit_id, domain_name, crawler_module, status)
                select 'bulk-' || g, 'd' || g || '.example', 'web', 'RESERVED'
                from   generate_series(1, %d) g
                """.formatted(rows)).update();
        List<String> ids = IntStream.rangeClosed(1, rows).mapToObj(i -> "bulk-" + i).toList();

        assertThat(source(5).acknowledge(ids)).isEqualTo(rows);

        assertThat(countWhere("status = 'DONE'")).isEqualTo(rows);
    }

    @Test
    void acknowledge_worksAfterClose() {
        seed("web", 3);
        DatabaseItemSource source = source(100);
        List<String> leased = source.getItems().stream().map(VisitRequest::getVisitId).toList();

        // PipelineService closes the source as soon as the producer is done, before the writer flushed.
        source.close();

        assertThat(source.acknowledge(leased)).isEqualTo(3);
        assertThat(countWhere("status = 'DONE'")).isEqualTo(3);
    }

    @Test
    void doneRows_areNeverLeasedAgain() {
        seed("web", 4);
        DatabaseItemSource source = source(2);
        List<String> firstPoll = source.getItems().stream().map(VisitRequest::getVisitId).toList();
        assertThat(firstPoll).hasSize(2);
        source.acknowledge(firstPoll);

        List<String> secondPoll = source.getItems().stream().map(VisitRequest::getVisitId).toList();

        assertThat(secondPoll).hasSize(2).doesNotContainAnyElementsOf(firstPoll);
        assertThat(source.getItems()).isEmpty();
        assertThat(countWhere("status = 'DONE'")).isEqualTo(2);
    }

    // --- helpers -------------------------------------------------------------------------

    private void seed(String module, int rows) {
        seedPendingTasks(jdbcClient, module, rows);
    }

    private void insertTask(String visitId, String module, String status) {
        jdbcClient.sql("insert into crawl_tasks (visit_id, domain_name, crawler_module, status) "
                        + "values (:visitId, :domain, :module, :status)")
                .param("visitId", visitId)
                .param("domain", visitId + ".example")
                .param("module", module)
                .param("status", status)
                .update();
    }

    private long countWhere(String predicate) {
        return countCrawlTasks(jdbcClient, predicate);
    }

    private Instant databaseNow() {
        return jdbcClient.sql("select now()").query(Timestamp.class).single().toInstant();
    }

    private List<Instant> finishedTimestamps(List<String> visitIds) {
        return jdbcClient.sql("select finished_timestamp from crawl_tasks where visit_id in (:ids) order by visit_id")
                .param("ids", visitIds)
                .query(Timestamp.class)
                .list()
                .stream()
                .map(Timestamp::toInstant)
                .toList();
    }

    private List<Map<String, Object>> reservationColumns(List<String> visitIds) {
        return jdbcClient.sql("""
                        select visit_id, reserved_by, reservation_id, reserved_timestamp, attempts
                        from   crawl_tasks
                        where  visit_id in (:ids)
                        order by visit_id
                        """)
                .param("ids", visitIds)
                .query()
                .listOfRows();
    }
}
