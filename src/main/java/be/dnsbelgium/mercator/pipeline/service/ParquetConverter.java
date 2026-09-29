package be.dnsbelgium.mercator.pipeline.service;

/**
 * Decoupling seam used by {@link JsonItemWriter} to turn a batch of JSON files into Parquet
 * without depending on the {@code persistence} layer.
 *
 * <p>Implementations receive a glob (e.g. {@code /path/to/batch-3/*.json}) that matches all
 * JSON files of a single batch and are responsible for reading those files and writing the
 * corresponding Parquet output in whatever layout is appropriate (module-specific typed
 * schema, partitioning, destination, ...).
 *
 * <p>A module's {@code repository::storeResults} is a natural {@code ParquetConverter}.
 */
@FunctionalInterface
public interface ParquetConverter {

    /**
     * Convert the JSON file(s) matched by {@code jsonGlob} to Parquet.
     *
     * @param jsonGlob a glob string matching the JSON files of a single batch
     * @throws RuntimeException if the conversion fails; {@link JsonItemWriter} treats this as
     *                          a recoverable per-batch failure and leaves the JSON on disk.
     */
    void convert(String jsonGlob);
}

