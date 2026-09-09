package be.dnsbelgium.mercator.integration;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.integration.ParquetDatasetComparator.ComparisonResult;
import be.dnsbelgium.mercator.persistence.BaseRepository;
import be.dnsbelgium.mercator.persistence.DnsRepository;
import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.pipeline.config.PipelineJacksonConfig;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the <b>new pipeline</b> writer produces Parquet output that is identical to the
 * <b>legacy Spring Batch</b> writer for the <code>web</code>, <code>dns</code> and
 * <code>tls</code> modules (smtp is covered separately later).
 *
 * <p><b>Approach (Option 1 of the t3 plan):</b> both writers ultimately delegate to the same
 * {@code repository.storeResults(...)} and the two modules share the same crawler processors, so
 * the only things that can differ are the JSON writer and its Jackson mapper (legacy
 * {@code com.fasterxml} vs new {@code tools.jackson}). To make the test fully deterministic we
 * take a fixed set of crawl results from {@link ObjectMother} (the "same input data") and feed the
 * <em>same objects</em> through both writers, each pointed at its own output location. Using mocks
 * of the live network is therefore unnecessary: the input is identical by construction.
 *
 * <p>The two datasets are then compared with {@link ParquetDatasetComparator} (schema + row set,
 * ignoring volatile timestamp columns) and the outcome is written to
 * {@code target/engine-parity-report.md}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EngineParityIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(EngineParityIntegrationTest.class);

    //private static final List<String> VOLATILE = List.of("crawl_started", "crawl_finished", "year", "month");
    private static final List<String> VOLATILE = List.of();
    private static final List<String> VOLATILE_RESPONSE_BODY = List.of();

    private final ObjectMother objectMother = new ObjectMother();

    // Legacy writer serializes with the @Primary com.fasterxml mapper; the new writer uses the
    // tools.jackson mapper. Both are the exact mappers used in production.
    private final com.fasterxml.jackson.databind.ObjectMapper legacyMapper = TestUtils.jsonReader();
    private final tools.jackson.databind.ObjectMapper pipelineMapper = new PipelineJacksonConfig().pipelineObjectMapper();

    private final ParquetDatasetComparator comparator = new ParquetDatasetComparator();
    private final ParityReport report = new ParityReport();

    @TempDir
    Path tempDir;

    @Test
    void web_output_is_identical() throws Exception {
        List<WebCrawlResult> results = List.of(objectMother.webCrawlResult1(), objectMother.webCrawlResult2());

        Path oldWeb = tempDir.resolve("web/old/web");
        Path oldRb = tempDir.resolve("web/old/web_response_body");
        Path newWeb = tempDir.resolve("web/new/web");
        Path newRb = tempDir.resolve("web/new/web_response_body");

        WebRepository oldRepo = new WebRepository(TestUtils.jdbcClientFactory(), legacyMapper, oldWeb.toString(), oldRb.toString());
        WebRepository newRepo = new WebRepository(TestUtils.jdbcClientFactory(), legacyMapper, newWeb.toString(), newRb.toString());

        writeThroughBothEngines(WebCrawlResult.class, results, oldRepo, newRepo);

        ComparisonResult web = comparator.compare("web", oldWeb, newWeb, VOLATILE);
        ComparisonResult responseBody = comparator.compare("web_response_body", oldRb, newRb, VOLATILE_RESPONSE_BODY);
        report.add(web);
        report.add(responseBody);

        assertIdentical(web);
        assertIdentical(responseBody);
    }

    @Test
    void dns_output_is_identical() throws Exception {
        List<DnsCrawlResult> results = List.of(
                objectMother.dnsCrawlResultWithMultipleResponses1("dnsbelgium.be", "1"),
                objectMother.dnsCrawlResultWithMultipleResponses2("example.be", "2"));

        Path oldBase = tempDir.resolve("dns/old");
        Path newBase = tempDir.resolve("dns/new");

        DnsRepository oldRepo = new DnsRepository(TestUtils.jdbcClientFactory(), legacyMapper, oldBase.toString(), false);
        DnsRepository newRepo = new DnsRepository(TestUtils.jdbcClientFactory(), legacyMapper, newBase.toString(), false);

        writeThroughBothEngines(DnsCrawlResult.class, results, oldRepo, newRepo);

        ComparisonResult dns = comparator.compare("dns", oldBase, newBase, VOLATILE);
        report.add(dns);
        assertIdentical(dns);
    }

    @Test
    void tls_output_is_identical() throws Exception {
        List<TlsCrawlResult> results = List.of(objectMother.tlsCrawlResult1(), objectMother.tlsCrawlResult2());

        Path oldBase = tempDir.resolve("tls/old");
        Path newBase = tempDir.resolve("tls/new");

        TlsRepository oldRepo = new TlsRepository(TestUtils.jdbcClientFactory(), legacyMapper, oldBase.toString());
        TlsRepository newRepo = new TlsRepository(TestUtils.jdbcClientFactory(), legacyMapper, newBase.toString());

        writeThroughBothEngines(TlsCrawlResult.class, results, oldRepo, newRepo);

        ComparisonResult tls = comparator.compare("tls", oldBase, newBase, VOLATILE);
        report.add(tls);
        assertIdentical(tls);
    }

    /**
     * Writes {@code results} to Parquet twice: once with the legacy Spring Batch
     * {@link be.dnsbelgium.mercator.batch.JsonItemWriter} (pointed at {@code oldRepo}) and once with
     * the new pipeline {@link be.dnsbelgium.mercator.pipeline.service.JsonItemWriter} (pointed at
     * {@code newRepo}). Both delegate to {@code repository.storeResults(...)}.
     */
    private <T> void writeThroughBothEngines(Class<T> clazz, List<T> results,
                                             BaseRepository<T> oldRepo, BaseRepository<T> newRepo) throws Exception {
        EngineParityWriters.writeThroughBothEngines(clazz, results, legacyMapper, pipelineMapper, oldRepo, newRepo, tempDir);
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
        Path reportPath = Path.of("target", "engine-parity-report.md");
        report.write(reportPath);
        logger.info("Engine parity report written to {} (all identical: {})",
                reportPath.toAbsolutePath(), report.allIdentical());
    }
}

