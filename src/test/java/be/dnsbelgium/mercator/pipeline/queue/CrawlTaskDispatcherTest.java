package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CrawlTaskDispatcher}: it fans each undispatched {@code visit_requests}
 * row into one {@code crawl_tasks} row per enabled module, stamps {@code dispatched_at}, and is
 * idempotent — re-running creates no duplicates for already-dispatched visits.
 */
@Slf4j
@Testcontainers
class CrawlTaskDispatcherTest {

    private static final List<String> MODULES = List.of("web", "dns", "smtp");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private JdbcClient jdbcClient;

    @BeforeAll
    static void createSchema() {
        JdbcClient client = newJdbcClient();
        client.sql("""
                create table visit_requests (
                    visit_id      text        not null,
                    domain_name   text        not null,
                    created_at    timestamptz not null default now(),
                    dispatched_at timestamptz,
                    dispatch_id   text
                )
                """).update();
        client.sql("""
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
        jdbcClient.sql("truncate table visit_requests").update();
        jdbcClient.sql("truncate table crawl_tasks").update();
    }

    private CrawlTaskDispatcher dispatcher() {
        PipelineProperties properties = new PipelineProperties();
        properties.getQueue().setCrawlerModules(MODULES);
        return new CrawlTaskDispatcher(jdbcClient, properties);
    }

    @Test
    void dispatch_fansEachVisitIntoOneRowPerModule() {
        seedVisits(3);

        dispatcher().dispatch();

        // 3 visits × 3 modules = 9 PENDING tasks, all dispatched.
        assertThat(count("crawl_tasks")).isEqualTo(9);
        assertThat(count("crawl_tasks where status = 'PENDING' and attempts = 0")).isEqualTo(9);
        for (String module : MODULES) {
            assertThat(count("crawl_tasks where crawler_module = '" + module + "'")).isEqualTo(3);
        }
        assertThat(count("visit_requests where dispatched_at is not null and dispatch_id is not null")).isEqualTo(3);
    }

    @Test
    void dispatch_isIdempotent_noDuplicatesForAlreadyDispatched() {
        seedVisits(3);

        dispatcher().dispatch();
        assertThat(count("crawl_tasks")).isEqualTo(9);

        dispatcher().dispatch();

        // Second run finds no undispatched visits → still exactly 9 rows.
        assertThat(count("crawl_tasks")).isEqualTo(9);
    }

    @Test
    void dispatch_pickUpNewlyAddedVisitsOnly() {
        seedVisits(2);
        dispatcher().dispatch();
        assertThat(count("crawl_tasks")).isEqualTo(6);

        // Add a new visit and re-dispatch: only the new one is fanned out (+3).
        insertVisit("v-new");
        dispatcher().dispatch();
        assertThat(count("crawl_tasks")).isEqualTo(9);
    }

    @Test
    void dispatch_noVisits_isNoOp() {
        dispatcher().dispatch();
        assertThat(count("crawl_tasks")).isZero();
    }

    // --- helpers -------------------------------------------------------------------------

    private void seedVisits(int n) {
        for (int i = 1; i <= n; i++) {
            insertVisit("v" + i);
        }
    }

    private void insertVisit(String visitId) {
        jdbcClient.sql("insert into visit_requests (visit_id, domain_name) values (:id, :domain)")
                .param("id", visitId)
                .param("domain", visitId + ".example")
                .update();
    }

    private long count(String fromWhere) {
        return jdbcClient.sql("select count(*) from " + fromWhere).query(Long.class).single();
    }

    private static JdbcClient newJdbcClient() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        return JdbcClient.create(new JdbcTemplate(ds));
    }
}
