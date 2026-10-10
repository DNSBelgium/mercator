package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.JsonItemWriter;
import be.dnsbelgium.mercator.pipeline.service.ParquetConverter;
import be.dnsbelgium.mercator.pipeline.service.PipelineService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.CRAWL_TASKS_DDL;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.countCrawlTasks;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.duckDbClient;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.jsonFilesRecursively;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.jsonToParquetConverter;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.parquetRowCountInDir;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.postgresJdbcClient;
import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.seedPendingTasks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end test of the "ack" step: a real {@link DatabaseItemSource} feeds a real
 * {@link PipelineService} whose {@link JsonItemWriter} reports every batch that reached Parquet back
 * to {@link DatabaseItemSource#acknowledge}. Only tasks whose result is in Parquet may become
 * {@code DONE}; everything else must stay {@code RESERVED}.
 */
@Testcontainers
class QueueAckPipelineTest {

    private static final String MODULE = "web";
    private static final int NUM_CONSUMERS = 4;

    /** Minimal module output: carries the visit id that links a Parquet row back to its task. */
    private record Result(String visitId, String domainName) { }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private JdbcClient queueDb;
    private JdbcClient duckDb;

    @BeforeAll
    static void createSchema() {
        postgresJdbcClient(POSTGRES).sql(CRAWL_TASKS_DDL).update();
    }

    @BeforeEach
    void setUp() {
        queueDb = postgresJdbcClient(POSTGRES);
        queueDb.sql("truncate table crawl_tasks").update();
        duckDb = duckDbClient();
    }

    @Test
    void happyPath_everyBatchIsAcked_includingTheFlushedRemainder() throws IOException {
        seedPendingTasks(queueDb, MODULE, 7);

        long produced = runPass(toResult(), parquetConverter(), 3);

        // 7 tasks with batchSize 3 -> batches of 3, 3 and a flushed remainder of 1: all are DONE.
        assertThat(produced).isEqualTo(7);
        assertThat(count("status = 'DONE' and finished_at is not null")).isEqualTo(7);
        assertThat(count("status <> 'DONE'")).isZero();
        assertThat(parquetRowCountInDir(duckDb, parquetDir())).isEqualTo(7);
        assertThat(jsonFilesRecursively(jsonDir())).isEmpty();
    }

    @Test
    void itemWithoutResult_staysReserved_whileTheOthersAreDone() {
        seedPendingTasks(queueDb, MODULE, 5);
        ItemProcessor<VisitRequest, Result> skipsOne =
                item -> "web-3".equals(item.getVisitId()) ? null : toResult().processItem(item);

        runPass(skipsOne, parquetConverter(), 2);

        assertThat(count("status = 'DONE'")).isEqualTo(4);
        assertThat(count("visit_id = 'web-3' and status = 'RESERVED' and finished_at is null")).isEqualTo(1);
        assertThat(parquetRowCountInDir(duckDb, parquetDir())).isEqualTo(4);
    }

    @Test
    void failedRollUp_leavesThatBatchReserved_laterBatchesAreDone_andThePassFails() throws IOException {
        seedPendingTasks(queueDb, MODULE, 7);
        ParquetConverter real = parquetConverter();
        AtomicInteger conversions = new AtomicInteger();
        ParquetConverter failsFirstBatch = glob -> {
            if (conversions.incrementAndGet() == 1) {
                throw new IllegalStateException("boom");
            }
            real.convert(glob);
        };

        // Batches (size 3): the first fails, the second (3 rows) and the flushed remainder (1 row) succeed.
        assertThatThrownBy(() -> runPass(toResult(), failsFirstBatch, 3))
                .hasRootCauseMessage("boom");

        assertThat(count("status = 'DONE'")).isEqualTo(4);
        assertThat(count("status = 'RESERVED' and finished_at is null")).isEqualTo(3);
        assertThat(parquetRowCountInDir(duckDb, parquetDir())).isEqualTo(4);
        // The failed batch's JSON is kept for inspection (existing policy).
        assertThat(jsonFilesRecursively(jsonDir())).hasSize(3);
    }

    @Test
    void otherModulesRows_areNeverTouched() {
        seedPendingTasks(queueDb, MODULE, 3);
        seedPendingTasks(queueDb, "dns", 3);

        runPass(toResult(), parquetConverter(), 2);

        assertThat(count("crawler_module = 'web' and status = 'DONE'")).isEqualTo(3);
        assertThat(count("crawler_module = 'dns' and status = 'PENDING' and finished_at is null"))
                .isEqualTo(3);
    }

    // --- helpers -------------------------------------------------------------------------

    private Path jsonDir() {
        return tempDir.resolve("json");
    }

    private Path parquetDir() {
        return tempDir.resolve("parquet");
    }

    private ParquetConverter parquetConverter() {
        return jsonToParquetConverter(duckDb, parquetDir());
    }

    private static ItemProcessor<VisitRequest, Result> toResult() {
        return item -> new Result(item.getVisitId(), item.getDomainName());
    }

    private long count(String predicate) {
        return countCrawlTasks(queueDb, predicate);
    }

    /**
     * Runs one bounded pass exactly like {@code VisitRequestModule.run()}: the writer reports each
     * batch that reached Parquet to {@code source::acknowledge}.
     *
     * @return the number of items the producer read from the queue
     */
    private long runPass(ItemProcessor<VisitRequest, Result> processor, ParquetConverter converter, int batchSize) {
        PipelineProperties.Queue queueProperties = new PipelineProperties.Queue();
        queueProperties.setFetchSize(4);
        queueProperties.setInstanceId("test-host");
        DatabaseItemSource source = new DatabaseItemSource(MODULE, queueDb, queueProperties);
        JsonItemWriter<Result> writer = new JsonItemWriter<>(objectMapper, converter, jsonDir(), Result.class,
                batchSize, Result::visitId, source::acknowledge);

        PipelineProperties properties = new PipelineProperties();
        properties.setNumConsumers(NUM_CONSUMERS);
        properties.setMaxConcurrentRequests(NUM_CONSUMERS);

        ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService cpuPool = Executors.newFixedThreadPool(1);
        ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1);
        PipelineExecutors executors = new PipelineExecutors(ioExecutor, cpuPool, watchdog);
        PipelineService<VisitRequest, Result> pipeline = new PipelineService<>(MODULE, source, processor, writer,
                executors, properties, new SimpleMeterRegistry(), NUM_CONSUMERS);
        try {
            return pipeline.runPipeline(NUM_CONSUMERS);
        } finally {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }
    }
}

