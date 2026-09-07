package be.dnsbelgium.mercator.pipeline.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@link ItemWriter} that persists each item as an individual JSON file and, every
 * {@code batchSize} items, rolls the accumulated JSON files up into a single Parquet file
 * using DuckDB, deleting the consumed JSON files afterwards. {@link #flush()} converts the
 * final partial batch.
 *
 * <p><strong>Thread confinement:</strong> the pipeline drives a single writer thread, so
 * the batch bookkeeping ({@code currentBatch}, {@code batchCounter}) needs no
 * synchronization. Only {@code writeCount} is {@code volatile}, so progress can be read
 * safely from another thread (e.g. a metrics gauge).
 *
 * <p>If a roll-up fails, the JSON files of that batch are intentionally left on disk for
 * manual inspection (recovery is handled out of band); the writer clears its in-memory
 * batch and continues.
 *
 * @param <T> the item type serialized to JSON and Parquet
 */
@Slf4j
public class JsonItemWriter<T> implements ItemWriter<T> {

    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;
    private final Path outputDirectory;
    private final String name;
    private final int batchSize;

    private final List<Path> currentBatch;
    private int batchCounter = 1;
    private volatile int writeCount = 0;

    public JsonItemWriter(ObjectMapper objectMapper,
                          JdbcClient jdbcClient,
                          Path outputDirectory,
                          Class<T> clazz,
                          int batchSize) {
        this.objectMapper = objectMapper;
        this.jdbcClient = jdbcClient;
        this.outputDirectory = outputDirectory;
        this.name = clazz.getSimpleName();
        this.batchSize = batchSize;
        this.currentBatch = new ArrayList<>(batchSize);
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create output directory " + outputDirectory, e);
        }
        log.info("JsonItemWriter for {} writing to {} (batchSize={})", name, outputDirectory, batchSize);
    }

    @Override
    public void write(T item) {
        setMDC();
        try {
            Path jsonFile = outputDirectory.resolve(UUID.randomUUID() + ".json");
            objectMapper.writeValue(jsonFile.toFile(), item);
            currentBatch.add(jsonFile);
            // This runs in a single thread, so the increment is safe; we just want to be able to read it from another thread.
            //noinspection NonAtomicOperationOnVolatileField
            writeCount++;
            log.debug("Wrote item to {}", jsonFile);
            if (currentBatch.size() >= batchSize) {
                rollUpToParquet();
            }
        } finally {
            resetMDC();
        }
    }

    @Override
    public void flush() {
        setMDC();
        try {
            if (!currentBatch.isEmpty()) {
                rollUpToParquet();
            }
        } finally {
            resetMDC();
        }
    }

    @Override
    public int writtenItems() {
        return writeCount;
    }

    @SuppressWarnings("SameParameterValue")
    String nop(String s) {
        // IntelliJ can be freaking annoying about SQL Dialect warnings
        // And I find no way to disable them for this project, so just wrap the string in a no-op method
        return s;
    }

    /**
     * Converts the JSON files accumulated in the current batch into one Parquet file via
     * DuckDB, then deletes those JSON files. On failure, the JSON files are left in place
     * for manual inspection and the batch is cleared so the writer can continue.
     */
    private void rollUpToParquet() {
        String fileName = String.format(nop("batch_%04d.parquet"), batchCounter);
        Path parquetFile = outputDirectory.resolve(fileName);
        try {
            log.info("Rolling up {} JSON files into parquet", currentBatch.size());
            String jsonList = currentBatch.stream()
                    .map(JsonItemWriter::sqlLiteral)
                    .collect(Collectors.joining(", ", "[", "]"));
            String sql = "COPY (SELECT * FROM read_json_auto(" + jsonList + ")) "
                    + "TO " + sqlLiteral(parquetFile) + " (FORMAT PARQUET)";
            jdbcClient.sql(sql).update();
            log.info("Rolled up {} JSON files into {}", currentBatch.size(), parquetFile);
            deleteBatchFiles();
            batchCounter++;
        } catch (RuntimeException e) {
            log.error("Failed to roll up {} JSON files into {}; leaving JSON files for inspection",
                    currentBatch.size(), parquetFile, e);
        } finally {
            // On success the files are already deleted; on failure they are left in place.
            // Either way, start a fresh batch so we don't retry the same files indefinitely.
            currentBatch.clear();
        }
    }

    private void deleteBatchFiles() {
        for (Path jsonFile : currentBatch) {
            try {
                Files.deleteIfExists(jsonFile);
            } catch (IOException e) {
                log.warn("Could not delete JSON file {} after roll-up", jsonFile, e);
            }
        }
    }

    /** Renders an absolute path as a single-quoted SQL string literal, escaping quotes. */
    private static String sqlLiteral(Path path) {
        String absolute = path.toAbsolutePath().toString();
        return "'" + absolute.replace("'", "''") + "'";
    }

    private void setMDC() {
        MDC.put("name", name);
    }

    private void resetMDC() {
        MDC.remove("name");
    }
}
