package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.postgresJdbcClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for the schema bootstrap in {@link CrawlTaskDispatcher#init()}: a fresh database gets the
 * {@code finished_timestamp} column, and a database created before that column existed is migrated
 * in place (idempotently). Uses its own container because every test drops and recreates the tables.
 */
@SuppressWarnings("SqlResolve")
@Testcontainers
class CrawlTaskSchemaTest {

    private static final String LEGACY_CRAWL_TASKS_DDL = """
            create table crawl_tasks (
                visit_id            varchar not null,
                domain_name         varchar(255) not null,
                crawler_module      varchar(100) not null,
                reservation_id      varchar(100),
                reserved_by         varchar(100),
                reserved_timestamp  timestamp,
                status              varchar(100),
                attempts            int default 0
            )
            """;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        this.jdbcClient = postgresJdbcClient(POSTGRES);
        jdbcClient.sql("drop table if exists crawl_tasks").update();
        jdbcClient.sql("drop table if exists visit_requests").update();
    }

    private CrawlTaskDispatcher dispatcher() {
        return new CrawlTaskDispatcher(jdbcClient, new PipelineProperties());
    }

    @Test
    void init_createsFreshTable_withFinishedTimestamp() {
        dispatcher().init();

        assertThat(finishedTimestampType()).contains("timestamp without time zone");
        assertThat(lookupIndexExists()).isTrue();
    }

    @Test
    void init_addsFinishedTimestamp_toLegacyTable_andKeepsExistingRows() {
        jdbcClient.sql(LEGACY_CRAWL_TASKS_DDL).update();
        jdbcClient.sql("insert into crawl_tasks (visit_id, domain_name, crawler_module, status) "
                + "values ('v1', 'v1.example', 'web', 'RESERVED')").update();
        assertThat(finishedTimestampType()).isEmpty();

        dispatcher().init();

        assertThat(finishedTimestampType()).contains("timestamp without time zone");
        assertThat(jdbcClient.sql("select count(*) from crawl_tasks where status = 'RESERVED' "
                + "and finished_timestamp is null").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void init_isIdempotent() {
        dispatcher().init();

        assertThatCode(() -> dispatcher().init()).doesNotThrowAnyException();

        assertThat(finishedTimestampType()).contains("timestamp without time zone");
    }

    // --- helpers -------------------------------------------------------------------------

    private Optional<String> finishedTimestampType() {
        return jdbcClient.sql("""
                        select data_type
                        from   information_schema.columns
                        where  table_schema = current_schema()
                          and  table_name = 'crawl_tasks'
                          and  column_name = 'finished_timestamp'
                        """)
                .query(String.class)
                .optional();
    }

    private boolean lookupIndexExists() {
        return jdbcClient.sql("select count(*) from pg_indexes where tablename = 'crawl_tasks' "
                        + "and indexname = 'idx_crawl_tasks_module_visit'")
                .query(Long.class)
                .single() == 1;
    }
}

