package be.dnsbelgium.mercator.pipeline.web;

import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineJacksonConfig;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.CsvItemSourceFactory;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.web.WebCrawler;
import be.dnsbelgium.mercator.web.WebProcessor;
import be.dnsbelgium.mercator.web.domain.*;
import be.dnsbelgium.mercator.web.wappalyzer.TechnologyAnalyzer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SqlNoDataSourceInspection")
//@Disabled // until Jackson stuff is fixed
class WebPipelineTest {

    // The pipeline must serialize with the snake_case + Instant mapper so the typed
    // read_json(columns={...}) schema in storeResults populates every column correctly.
    private final ObjectMapper objectMapper = new PipelineJacksonConfig().pipelineObjectMapper();

    @Test
    void runsWebModuleEndToEnd_csvToParquet(@TempDir Path dir) throws IOException {
        // Given a small input CSV of visit requests
        Path csv = dir.resolve("input.csv");
        Files.writeString(csv, """
                domain_name,visit_id
                dnsbelgium.be,v0
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

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        PageFetcher pageFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.defaultConfig());
        WebProcessor webProcessor = getWebProcessor(meterRegistry, pageFetcher);

        // The repository decides the Parquet destination/layout (typed schema, year/month
        // partitioning) exactly like the legacy batch writer. Point it at the temp dir.
        Path webBase = dir.resolve("data").resolve("web");
        Path responseBodyBase = dir.resolve("data").resolve("web_response_body");
        WebRepository repository = new WebRepository(
                TestUtils.jdbcClientFactory(), TestUtils.jsonReader(),
                webBase.toString(), responseBodyBase.toString());

        WebPipeline webPipeline =
                new WebPipeline(
                        jdbcClient,
                        objectMapper,
                        executors,
                        properties,
                        meterRegistry,
                        webProcessor,
                        repository,
                        itemSourceFactory
                );

        // When the module runs to completion
        try {
            webPipeline.run();
        } finally {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }

        // Then all 5 results are persisted as Parquet in the repository base location,
        // Hive-partitioned by year/month, with no transient JSON left behind.
        Path transientJsonDir = dir.resolve("out").resolve("web");
        assertThat(jsonFilesRecursively(transientJsonDir)).isEmpty();

        assertThat(webBase).isDirectory();
        assertThat(parquetRowCountRecursive(jdbcClient, webBase)).isEqualTo(5);

        // The web module additionally produces the web_response_body dataset.
        assertThat(responseBodyBase).isDirectory();
        assertThat(parquetRowCountRecursive(jdbcClient, responseBodyBase)).isGreaterThan(0);
    }

    private static @NonNull WebProcessor getWebProcessor(MeterRegistry meterRegistry, PageFetcher pageFetcher) {
        VatFinder vatFinder = new VatFinder();
        VatLinkPrioritizer prioritizer = new VatLinkPrioritizer();
        VatScraper vatScraper = new VatScraper(meterRegistry, pageFetcher, vatFinder, prioritizer);
        HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(meterRegistry,false);
        TechnologyAnalyzer te = new TechnologyAnalyzer(meterRegistry);
        WebCrawler webCrawler = new WebCrawler(vatScraper, meterRegistry, featureExtractor, te);

        return new WebProcessor(webCrawler);
    }
}
