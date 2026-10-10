package be.dnsbelgium.mercator.pipeline.module;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.BaseRepository;
import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

/**
 * Shared base for modules that read {@link VisitRequest}s from a source (CSV by default,
 * or the Postgres work queue under the {@code postgres-queue} profile) and write their
 * results as JSON-then-Parquet. A concrete module only needs to supply its
 * {@link #name()}, its {@link #processor()}, its {@link #outputType()} and how to read the
 * visit id off a result ({@link #visitIdOf}) — everything
 * else (source, writer, engine wiring, graceful shutdown) is inherited, demonstrating how
 * little per-module code the generic engine requires.
 *
 * @param <O> the module's output/result type
 */
@Slf4j
public abstract class VisitRequestModule<O> implements PipelineModule {

    protected final JdbcClient jdbcClient;
    protected final ObjectMapper objectMapper;
    protected final PipelineExecutors executors;
    protected final PipelineProperties properties;
    protected final MeterRegistry meterRegistry;

    /**
     * The module's repository. Its {@code storeResults} is used as the {@link ParquetConverter}
     * so that JSON batches are converted to Parquet exactly like the legacy batch writer
     * (typed schema, {@code year}/{@code month} partitioning, repository base location).
     */
    protected final BaseRepository<O> repository;

    /**
     * Injected source factory. Spring picks the CSV-backed factory by default, or the
     * {@code @Primary} Postgres factory when the {@code postgres-queue} profile is active.
     * The module itself stays agnostic of both.
     */
    protected final ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory;

    protected VisitRequestModule(JdbcClient jdbcClient,
                                 ObjectMapper objectMapper,
                                 PipelineExecutors executors,
                                 PipelineProperties properties,
                                 MeterRegistry meterRegistry,
                                 BaseRepository<O> repository,
                                 ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.executors = executors;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.repository = repository;
        this.itemSourceFactory = itemSourceFactory;
    }

    /** The module's processor: turns a {@link VisitRequest} into a result of type {@code O}. */
    protected abstract ItemProcessor<VisitRequest, O> processor();

    /** The result class, needed by the JSON writer for typing/diagnostics. */
    protected abstract Class<O> outputType();

    /**
     * The id of the {@link VisitRequest} a result was produced for (its {@code visitId}). Once the
     * result is in Parquet, the writer reports this id to {@link ItemSource#acknowledge} so a
     * stateful source (the Postgres work queue) can close the corresponding task.
     */
    protected abstract String visitIdOf(O result);

    @Override
    public long run() {
        ItemSource<VisitRequest> source = itemSourceFactory.create(name());
        Path outputDir = Path.of(properties.getOutputDirectory(), name());
        // Delegate the JSON -> Parquet conversion to the module's repository.storeResults,
        // reproducing the legacy batch writer's typed schema / partitioning / destination.
        // After each batch is in Parquet, the source is told which visits are done (ack).
        ItemWriter<O> writer = new JsonItemWriter<>(objectMapper, repository::storeResults, outputDir,
                outputType(), properties.getBatchSize(), this::visitIdOf, source::acknowledge);

        PipelineService<VisitRequest, O> pipeline =
                new PipelineService<>(name(), source, processor(), writer, executors, properties, meterRegistry,
                        properties.maxConcurrentRequests(name()));

        int numConsumers = properties.numConsumers(name());
        log.info("Starting module '{}' (input={}, output={}, consumers={}, maxConcurrentRequests={})",
                name(), properties.getInputCsv(), outputDir, numConsumers,
                properties.maxConcurrentRequests(name()));
        return pipeline.runPipeline(numConsumers);
    }

}
