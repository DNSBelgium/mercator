package be.dnsbelgium.mercator.integration;

import be.dnsbelgium.mercator.MercatorApplication;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlService;
import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.integration.ParquetDatasetComparator.ComparisonResult;
import be.dnsbelgium.mercator.persistence.DnsRepository;
import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.pipeline.config.PipelineJacksonConfig;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import be.dnsbelgium.mercator.web.WebProcessor;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same parity check as {@link EngineParityIntegrationTest}, but instead of deterministic
 * {@code ObjectMother} objects it feeds <b>real crawl results</b> through both writers.
 *
 * <p>The real crawlers (network-dependent, slow and non-deterministic across runs) are
 * <b>opt-in</b>: this test only runs when the environment variable
 * {@code PARITY_REAL_CRAWL_ENABLED=true} is set. It autowires the fully-wired processor beans
 * (so no fragile manual wiring of resolver/scanner/cache/… is needed) from the
 * {@link MercatorApplication} context under the {@code test} profile. Because
 * {@code PipelineRunner} is annotated {@code @Profile("!test")}, booting this context does
 * <b>not</b> kick off the pipeline.
 *
 * <p>Each domain is crawled <b>once</b>; the resulting objects are written by both the legacy and
 * the new writer, so the input is identical by construction and any difference reflects a real
 * writer/serialization discrepancy. Volatile timestamp columns are ignored (each run crawls at a
 * different wall-clock time). The outcome is written to
 * {@code target/engine-parity-report-real-crawl.md}.
 */
@SpringBootTest(classes = MercatorApplication.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PARITY_REAL_CRAWL_ENABLED", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EngineParityRealCrawlIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(EngineParityRealCrawlIntegrationTest.class);

    private static final List<String> VOLATILE = List.of();
    //private static final List<String> VOLATILE = List.of("crawl_started", "crawl_finished", "year", "month");
    private static final List<String> VOLATILE_RESPONSE_BODY = List.of();

    /** Domains to crawl for real. Kept small since real crawls are slow. */
    private static final List<VisitRequest> INPUT = List.of(new VisitRequest("v1", "dnsbelgium.be"));

    @Autowired
    private WebProcessor webProcessor;
    @Autowired
    private DnsCrawlService dnsCrawlService;
    @Autowired
    private TlsCrawler tlsCrawler;

    private final com.fasterxml.jackson.databind.ObjectMapper legacyMapper = TestUtils.jsonReader();
    private final tools.jackson.databind.ObjectMapper pipelineMapper = new PipelineJacksonConfig().pipelineObjectMapper();

    private final ParquetDatasetComparator comparator = new ParquetDatasetComparator();
    private final ParityReport report = new ParityReport();

    @TempDir
    Path tempDir;

    @Test
    void web_real_crawl_output_is_identical() throws Exception {
        List<WebCrawlResult> results = crawlAll(webProcessor);
        assertThat(results).as("real web crawl should produce results").isNotEmpty();

        Path oldWeb = tempDir.resolve("web/old/web");
        Path oldRb = tempDir.resolve("web/old/web_response_body");
        Path newWeb = tempDir.resolve("web/new/web");
        Path newRb = tempDir.resolve("web/new/web_response_body");

        WebRepository oldRepo = new WebRepository(TestUtils.jdbcClientFactory(), legacyMapper, oldWeb.toString(), oldRb.toString());
        WebRepository newRepo = new WebRepository(TestUtils.jdbcClientFactory(), legacyMapper, newWeb.toString(), newRb.toString());
        EngineParityWriters.writeThroughBothEngines(WebCrawlResult.class, results, legacyMapper, pipelineMapper, oldRepo, newRepo, tempDir);

        ComparisonResult web = comparator.compare("web", oldWeb, newWeb, VOLATILE);
        ComparisonResult responseBody = comparator.compare("web_response_body", oldRb, newRb, VOLATILE_RESPONSE_BODY);
        report.add(web);
        report.add(responseBody);

        assertIdentical(web);
        assertIdentical(responseBody);
    }

    @Test
    void dns_real_crawl_output_is_identical() throws Exception {
        List<DnsCrawlResult> results = crawlAll(dnsCrawlService);
        assertThat(results).as("real dns crawl should produce results").isNotEmpty();

        Path oldBase = tempDir.resolve("dns/old");
        Path newBase = tempDir.resolve("dns/new");

        DnsRepository oldRepo = new DnsRepository(TestUtils.jdbcClientFactory(), legacyMapper, oldBase.toString(), false);
        DnsRepository newRepo = new DnsRepository(TestUtils.jdbcClientFactory(), legacyMapper, newBase.toString(), false);
        EngineParityWriters.writeThroughBothEngines(DnsCrawlResult.class, results, legacyMapper, pipelineMapper, oldRepo, newRepo, tempDir);

        ComparisonResult dns = comparator.compare("dns", oldBase, newBase, VOLATILE);
        report.add(dns);
        assertIdentical(dns);
    }

    @Test
    void tls_real_crawl_output_is_identical() throws Exception {
        List<TlsCrawlResult> results = crawlAll(tlsCrawler);
        assertThat(results).as("real tls crawl should produce results").isNotEmpty();

        Path oldBase = tempDir.resolve("tls/old");
        Path newBase = tempDir.resolve("tls/new");

        TlsRepository oldRepo = new TlsRepository(TestUtils.jdbcClientFactory(), legacyMapper, oldBase.toString());
        TlsRepository newRepo = new TlsRepository(TestUtils.jdbcClientFactory(), legacyMapper, newBase.toString());
        EngineParityWriters.writeThroughBothEngines(TlsCrawlResult.class, results, legacyMapper, pipelineMapper, oldRepo, newRepo, tempDir);

        ComparisonResult tls = comparator.compare("tls", oldBase, newBase, VOLATILE);
        report.add(tls);
        assertIdentical(tls);
    }

    /** Crawls every {@link #INPUT} request once with the given processor, dropping null results. */
    private <T> List<T> crawlAll(ItemProcessor<VisitRequest, T> processor) {
        List<T> results = new ArrayList<>();
        for (VisitRequest request : INPUT) {
            T result = processor.processItem(request);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    private void assertIdentical(ComparisonResult r) {
        assertThat(r.schemaMatches())
                .as("Schema of '%s' should match. old=%s new=%s", r.label(), r.oldSchema(), r.newSchema())
                .isTrue();
        assertThat(r.onlyInOld())
                .as("Rows present only in the legacy output for '%s'", r.label())
                .isEmpty();
        assertThat(r.onlyInNew())
                .as("Rows present only in the new pipeline output for '%s'", r.label())
                .isEmpty();
    }

    @AfterAll
    void writeReport() throws Exception {
        Path reportPath = Path.of("target", "engine-parity-report-real-crawl.md");
        report.write(reportPath);
        logger.info("Real-crawl engine parity report written to {} (all identical: {})",
                reportPath.toAbsolutePath(), report.allIdentical());
    }
}


