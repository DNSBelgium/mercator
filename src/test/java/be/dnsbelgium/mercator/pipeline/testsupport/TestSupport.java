package be.dnsbelgium.mercator.pipeline.testsupport;

import be.dnsbelgium.mercator.pipeline.service.ParquetConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Small, shared helpers for tests. Each call to {@link #duckDbClient()} opens a fresh
 * in-memory DuckDB connection, which is fine for tests that only read/write files on disk.
 */
public final class TestSupport {

    /**
     * The {@code crawl_tasks} table as used by the Postgres queue tests (same columns as the
     * production DDL in {@code CrawlTaskDispatcher.init()}, with stricter types/defaults).
     */
    public static final String CRAWL_TASKS_DDL = """
            create table crawl_tasks (
                visit_id           text        not null,
                domain_name        text        not null,
                crawler_module     text        not null,
                status             text        not null default 'PENDING',
                reserved_at        timestamptz,
                reserved_by        text,
                reservation_id     text,
                attempts           int         not null default 0,
                finished_at        timestamptz
            )
            """;

    private TestSupport() {
    }

    public static JdbcClient duckDbClient() {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:duckdb:");
        ds.setDriverClassName("org.duckdb.DuckDBDriver");
        return JdbcClient.create(ds);
    }

    /** A fresh {@link JdbcClient} (new connection per statement) for the given Testcontainers Postgres. */
    public static JdbcClient postgresJdbcClient(PostgreSQLContainer postgres) {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        return JdbcClient.create(new JdbcTemplate(ds));
    }

    /** Inserts {@code rows} {@code PENDING} tasks for {@code module}, with visit ids {@code <module>-1..<rows>}. */
    public static void seedPendingTasks(JdbcClient client, String module, int rows) {
        for (int i = 1; i <= rows; i++) {
            client.sql("insert into crawl_tasks (visit_id, domain_name, crawler_module, status, attempts) "
                            + "values (:visitId, :domain, :module, 'PENDING', 0)")
                    .param("visitId", module + "-" + i)
                    .param("domain", "d" + i + ".example")
                    .param("module", module)
                    .update();
        }
    }

    /** Counts the {@code crawl_tasks} rows matching the given SQL predicate. */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public static long countCrawlTasks(JdbcClient client, String predicate) {
        return client.sql("select count(*) from crawl_tasks where " + predicate)
                .query(Long.class)
                .single();
    }

    /**
     * A {@link ParquetConverter} that turns the JSON files matched by the glob into one Parquet file in
     * {@code parquetDir} (via DuckDB's {@code read_json_auto}), so tests can count the resulting rows.
     */
    public static ParquetConverter jsonToParquetConverter(JdbcClient duckDb, Path parquetDir) {
        try {
            Files.createDirectories(parquetDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return jsonGlob -> {
            Path parquet = parquetDir.resolve(UUID.randomUUID() + ".parquet");
            //noinspection SqlSourceToSinkFlow
            duckDb.sql("COPY (SELECT * FROM read_json_auto('" + jsonGlob + "')) "
                    + "TO '" + parquet.toAbsolutePath() + "' (FORMAT PARQUET)").update();
        };
    }

    /**
     * Recursively collects every {@code *.json} file under {@code dir} (any depth). Returns an
     * empty list when {@code dir} does not exist.
     */
    public static List<Path> jsonFilesRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (var stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json")).toList();
        }
    }

    /** Counts rows across every {@code *.parquet} file under {@code dir}, recursively (any depth). */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public static long parquetRowCountRecursive(JdbcClient client, Path dir) {
        return client.sql("SELECT count(*) FROM read_parquet('" + dir.toAbsolutePath() + "/**/*.parquet')")
                .query(Long.class)
                .single();
    }

    /** Counts rows across every {@code *.parquet} file in {@code dir}. */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public static long parquetRowCountInDir(JdbcClient client, Path dir) {
        return client.sql("SELECT count(*) FROM read_parquet('" + dir.toAbsolutePath() + "/*.parquet')")
                .query(Long.class)
                .single();
    }
}
