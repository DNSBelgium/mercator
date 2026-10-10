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
import java.util.function.Function;

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
 * the batch bookkeeping ({@code currentBatch}, {@code currentBatchIds}, {@code currentBatchDir},
 * {@code batchCounter}) needs no synchronization. Only {@code writeCount} is {@code volatile},
 * so progress can be read safely from another thread (e.g. a metrics gauge). The
 * {@link BatchCommitListener} is invoked on this same writer thread.
 *
 * <p>If a roll-up fails, the JSON files of that batch are intentionally left on disk for
 * manual inspection. The writer remembers the first failure, continues draining the result
 * queue, and propagates it from {@link #flush()} so the batch run cannot report success.
 *
 * <p><strong>Commit notification:</strong> when constructed with an id extractor and a
 * {@link BatchCommitListener}, the writer remembers the <em>ids</em> (never the items) of the
 * current batch and, after the batch was successfully converted to Parquet, reports exactly
 * those ids to the listener. A failed roll-up never notifies the listener. A failing listener
 * does not affect the Parquet output or the JSON clean-up (the data is already stored); its
 * first failure is remembered and rethrown from {@link #flush()}, like a roll-up failure.
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
    private final Function<T, String> idExtractor;
    private final BatchCommitListener commitListener;

    private final List<Path> currentBatch;
    private final List<String> currentBatchIds;
    private Path currentBatchDir;
    private int batchCounter = 1;
    private volatile int writeCount = 0;
    private RuntimeException rollUpFailure;
    private RuntimeException commitFailure;

    /** Creates a writer that does not report committed batches. */
    public JsonItemWriter(ObjectMapper objectMapper,
                          ParquetConverter converter,
                          Path outputDirectory,
                          Class<T> clazz,
                          int batchSize) {
        this(objectMapper, converter, outputDirectory, clazz, batchSize, null, null);
    }

    /**
     * Creates a writer that reports every batch converted to Parquet to {@code commitListener}.
     *
     * @param idExtractor    extracts the id of an item (items with a {@code null} id are not reported);
     *                       must be {@code null} iff {@code commitListener} is {@code null}
     * @param commitListener receives the ids of each batch after it was converted to Parquet
     */
    public JsonItemWriter(ObjectMapper objectMapper,
                          ParquetConverter converter,
                          Path outputDirectory,
                          Class<T> clazz,
                          int batchSize,
                          Function<T, String> idExtractor,
                          BatchCommitListener commitListener) {
        if ((idExtractor == null) != (commitListener == null)) {
            throw new IllegalArgumentException("idExtractor and commitListener must be both set or both null");
        }
        this.objectMapper = objectMapper;
        this.converter = converter;
        this.outputDirectory = outputDirectory;
        this.name = clazz.getSimpleName();
        this.batchSize = batchSize;
        this.idExtractor = idExtractor;
        this.commitListener = commitListener;
        this.currentBatch = new ArrayList<>(batchSize);
        this.currentBatchIds = new ArrayList<>(idExtractor == null ? 0 : batchSize);
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
            recordId(item);
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
            if (rollUpFailure != null) {
                IllegalStateException failure = new IllegalStateException(
                        "One or more JSON batches could not be converted to Parquet", rollUpFailure);
                if (commitFailure != null) {
                    failure.addSuppressed(commitFailure);
                }
                throw failure;
            }
            if (commitFailure != null) {
                throw new IllegalStateException(
                        "One or more batches were written to Parquet but could not be marked done", commitFailure);
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
     * directory. On failure, the JSON files are left in place (for manual inspection) and the first conversion failure
     * is retained for propagation from {@link #flush()}; the commit listener is not notified.
     * After a successful conversion the ids of the batch are reported to the listener.
     */
    private void rollUpToParquet() {
        Path batchDir = currentBatchDir;
        String glob = batchDir.toAbsolutePath() + "/*.json";
        List<String> batchIds = List.copyOf(currentBatchIds);
        boolean converted = false;
        try {
            log.info("Rolling up {} JSON files matching {} into parquet", currentBatch.size(), glob);
            converter.convert(glob);
            converted = true;
            log.info("Rolled up {} JSON files from {}", currentBatch.size(), batchDir);
            deleteBatchFiles();
            deleteBatchDir();
        } catch (RuntimeException e) {
            log.error("Failed to roll up {} JSON files from {}; leaving JSON files for inspection",
                    currentBatch.size(), batchDir, e);
            if (rollUpFailure == null) {
                rollUpFailure = e;
            }
        } finally {
            // On success the files are already deleted; on failure they are left in place.
            // Either way, advance to a fresh batch directory so we don't retry the same files.
            currentBatch.clear();
            currentBatchIds.clear();
            currentBatchDir = null;
            batchCounter++;
        }
        if (converted) {
            notifyCommitted(batchIds, batchDir);
        }
    }

    /** Remembers the id of the item just written to the current batch, if commit reporting is enabled. */
    private void recordId(T item) {
        if (idExtractor == null) {
            return;
        }
        String id = idExtractor.apply(item);
        if (id == null) {
            log.warn("Item written to {} has no id; it will not be reported as committed", currentBatchDir);
            return;
        }
        currentBatchIds.add(id);
    }

    /**
     * Reports a batch that is now in Parquet to the commit listener. A listener failure is logged
     * and remembered (first one wins) for {@link #flush()}; it must not undo the roll-up.
     */
    private void notifyCommitted(List<String> batchIds, Path batchDir) {
        if (batchIds.isEmpty()) {
            return;
        }
        try {
            commitListener.onBatchCommitted(batchIds);
        } catch (RuntimeException e) {
            log.error("{} item(s) from {} were written to parquet but could not be marked done",
                    batchIds.size(), batchDir, e);
            if (commitFailure == null) {
                commitFailure = e;
            }
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
