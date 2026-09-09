package be.dnsbelgium.mercator.integration;

import be.dnsbelgium.mercator.persistence.BaseRepository;
import org.springframework.batch.infrastructure.item.Chunk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes the same list of results to Parquet twice: once through the legacy Spring Batch
 * {@link be.dnsbelgium.mercator.batch.JsonItemWriter} and once through the new pipeline
 * {@link be.dnsbelgium.mercator.pipeline.service.JsonItemWriter}. Both delegate to
 * {@code repository.storeResults(...)}; only the writer and its Jackson mapper differ.
 *
 * <p>Shared by {@link EngineParityIntegrationTest} (deterministic {@code ObjectMother} input) and
 * {@link EngineParityRealCrawlIntegrationTest} (real crawl input).
 */
final class EngineParityWriters {

    private EngineParityWriters() {
    }

    static <T> void writeThroughBothEngines(
            Class<T> clazz,
            List<T> results,
            com.fasterxml.jackson.databind.ObjectMapper legacyMapper,
            tools.jackson.databind.ObjectMapper pipelineMapper,
            BaseRepository<T> oldRepo,
            BaseRepository<T> newRepo,
            Path workDir) throws Exception {

        // --- Legacy Spring Batch writer: one JSON array file per chunk, storeResults on close(). ---
        Path oldJsonDir = Files.createTempDirectory(workDir, "old-json-");
        var oldWriter = new be.dnsbelgium.mercator.batch.JsonItemWriter<>(oldRepo, legacyMapper, oldJsonDir, clazz);
        Chunk<T> chunk = new Chunk<>();
        results.forEach(chunk::add);
        oldWriter.write(chunk);
        oldWriter.close();

        // --- New pipeline writer: one JSON object per file, storeResults per batch on flush(). ---
        Path newJsonDir = Files.createTempDirectory(workDir, "new-json-");
        var newWriter = new be.dnsbelgium.mercator.pipeline.service.JsonItemWriter<>(
                pipelineMapper, newRepo::storeResults, newJsonDir, clazz, Math.max(1, results.size()));
        for (T result : results) {
            newWriter.write(result);
        }
        newWriter.flush();
    }
}

