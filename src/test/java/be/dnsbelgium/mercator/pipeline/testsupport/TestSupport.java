package be.dnsbelgium.mercator.pipeline.testsupport;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Small, shared helpers for tests. Each call to {@link #duckDbClient()} opens a fresh
 * in-memory DuckDB connection, which is fine for tests that only read/write files on disk.
 */
public final class TestSupport {

    private TestSupport() {
    }

    public static JdbcClient duckDbClient() {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:duckdb:");
        ds.setDriverClassName("org.duckdb.DuckDBDriver");
        return JdbcClient.create(ds);
    }

    public static List<Path> filesWithSuffix(Path dir, String suffix) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(suffix)).toList();
        }
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

    /** Counts rows in a single Parquet file. */
    public static long parquetRowCount(JdbcClient client, Path parquetFile) {
        //noinspection SqlSourceToSinkFlow
        return client.sql("SELECT count(*) FROM read_parquet('" + parquetFile.toAbsolutePath() + "')")
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
