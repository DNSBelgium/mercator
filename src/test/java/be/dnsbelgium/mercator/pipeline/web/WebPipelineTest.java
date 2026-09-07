package be.dnsbelgium.mercator.pipeline.web;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.CsvItemSourceFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SqlNoDataSourceInspection")
@Disabled // until Jackson stuff is fixed
class WebPipelineTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void runsWebModuleEndToEnd_csvToParquet(@TempDir Path dir) throws IOException {
        // Given a small input CSV of visit requests
        Path csv = dir.resolve("input.csv");
        Files.writeString(csv, """
                domain_name,visit_id
                example0.be,v0
                example1.be,v1
                example2.be,v2
                example3.be,v3
                example4.be,v4
                """);

        JdbcClient jdbcClient = duckDbClient();

        PipelineProperties properties = new PipelineProperties();
        properties.setInputCsv(csv.toString());
        properties.setOutputDirectory(dir.resolve("out").toString());
        properties.setBatchSize(2);
        properties.setNumConsumers(4);
        properties.setMaxConcurrentRequests(8);

        ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService cpuPool = Executors.newFixedThreadPool(2);
        ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1);
        PipelineExecutors executors = new PipelineExecutors(ioExecutor, cpuPool, watchdog);


        // The default CSV-backed factory is the one Spring injects when no queue profile is active.
        CsvItemSourceFactory itemSourceFactory = new CsvItemSourceFactory(jdbcClient, properties);

        WebPipeline webPipeline =
                new WebPipeline(jdbcClient, objectMapper, executors, properties,
                        new SimpleMeterRegistry(), new WebProcessor(), itemSourceFactory);

        // When the module runs to completion
        try {
            webPipeline.run();
        } finally {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }

        // Then all 5 results are persisted as Parquet, with no JSON left behind
        Path webOut = dir.resolve("out").resolve("web");
        assertThat(webOut).isDirectory();
        assertThat(filesWithSuffix(webOut, ".json")).isEmpty();
        assertThat(filesWithSuffix(webOut, ".parquet")).isNotEmpty();
        assertThat(parquetRowCountInDir(jdbcClient, webOut)).isEqualTo(5);
    }
}
