package be.dnsbelgium.mercator.pipeline.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link ItemWriter} that persists each item as an individual JSON file and, every
 * {@code batchSize} items, rolls the accumulated JSON files up into Parquet by delegating to
 * a {@link ParquetConverter}, deleting the consumed JSON files afterwards. {@link #flush()}
 * converts the final partial batch.
 *
 * <p>Each batch is written into its own sub-directory ({@code <outputDirectory>/batch-<n>/})
 * so the converter receives a glob that matches exactly the files of that batch and never
 * re-ingests files left behind by a previously failed batch.
 *
 * <p><strong>Thread confinement:</strong> the pipeline drives a single writer thread, so
 * the batch bookkeeping ({@code currentBatch}, {@code currentBatchDir}, {@code batchCounter})
 * needs no synchronization. Only {@code writeCount} is {@code volatile}, so progress can be
 * read safely from another thread (e.g. a metrics gauge).
 *
 * <p>If a roll-up fails, the JSON files of that batch are intentionally left on disk for
 * manual inspection (recovery is handled out of band); the writer clears its in-memory batch,
 * moves on to a fresh batch directory and continues.
 *
 * @param <T> the item type serialized to JSON and Parquet
 */
@Slf4j
public class JsonItemWriter<T> implements ItemWriter<T> {

    private final ObjectMapper objectMapper;
    private final ParquetConverter converter;
    private final Path outputDirectory;
    private final String name;
    private final int batchSize;

    private final List<Path> currentBatch;
    private Path currentBatchDir;
    private int batchCounter = 1;
    private volatile int writeCount = 0;

    public JsonItemWriter(ObjectMapper objectMapper,
                          ParquetConverter converter,
                          Path outputDirectory,
                          Class<T> clazz,
                          int batchSize) {
        this.objectMapper = objectMapper;
        this.converter = converter;
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
            if (currentBatch.isEmpty()) {
                startNewBatchDir();
            }
            Path jsonFile = currentBatchDir.resolve(UUID.randomUUID() + ".json");
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

    private void startNewBatchDir() {
        currentBatchDir = outputDirectory.resolve("batch-" + batchCounter);
        try {
            Files.createDirectories(currentBatchDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create batch directory " + currentBatchDir, e);
        }
    }

    /**
     * Converts the JSON files accumulated in the current batch into Parquet by delegating to
     * the {@link ParquetConverter}, then deletes those JSON files and their (now empty) batch
     * directory. On failure, the JSON files are left in place for manual inspection and the
     * writer advances to a fresh batch directory so it can continue.
     */
    private void rollUpToParquet() {
        String glob = currentBatchDir.toAbsolutePath() + "/*.json";
        try {
            log.info("Rolling up {} JSON files matching {} into parquet", currentBatch.size(), glob);
            converter.convert(glob);
            log.info("Rolled up {} JSON files from {}", currentBatch.size(), currentBatchDir);
            deleteBatchFiles();
            deleteBatchDir();
        } catch (RuntimeException e) {
            log.error("Failed to roll up {} JSON files from {}; leaving JSON files for inspection",
                    currentBatch.size(), currentBatchDir, e);
        } finally {
            // On success the files are already deleted; on failure they are left in place.
            // Either way, advance to a fresh batch directory so we don't retry the same files.
            currentBatch.clear();
            currentBatchDir = null;
            batchCounter++;
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

    private void deleteBatchDir() {
        try {
            Files.deleteIfExists(currentBatchDir);
        } catch (IOException e) {
            log.warn("Could not delete batch directory {} after roll-up", currentBatchDir, e);
        }
    }

    private void setMDC() {
        MDC.put("name", name);
    }

    private void resetMDC() {
        MDC.remove("name");
    }
}
