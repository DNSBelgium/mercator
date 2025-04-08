package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DnsRepositoryTest {

    private static final Logger logger = LoggerFactory.getLogger(DnsRepositoryTest.class);

    @TempDir
    static Path baseLocation;

    @TempDir
    static Path tempDir;

    static {
        if (System.getProperty("mercator_temp_dir") != null) {
            // this allows to run the tests with a folder that does not disappear after the test completes.
            baseLocation = Path.of(System.getProperty("mercator_temp_dir"), UUID.randomUUID().toString());
            logger.info("Using base location {}", baseLocation);
        }
    }
    private final ObjectMother objectMother = new ObjectMother();
    private final DnsRepository repository = new DnsRepository(TestUtils.jsonReader(), baseLocation.toString());



    @Test
    @EnabledIfEnvironmentVariable(named = "S3_TEST_ENABLED", matches = "True")
    public void toS3Parquet() throws IOException {

        DnsRepository s3DnsRepository = new DnsRepository(TestUtils.jsonReader(), System.getProperty("mercator_s3_base_path"));

        logger.info("tempDir = {}", baseLocation);
        Files.createDirectories(baseLocation);
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses();

        logger.info("DnsCrawlResultResult = {}", dnsCrawlResultResult1);

        File jsonFile = tempDir.resolve("dnsCrawlResultResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(dnsCrawlResultResult1));

        s3DnsRepository.storeResults(jsonFile.toString());

        List<DnsCrawlResult> dnsCrawlResultResults = s3DnsRepository.findByDomainName("example.com");
        logger.info("dnsCrawlResultResults found: {}", dnsCrawlResultResults.size());
        logger.info("dnsCrawlResultResults = {}", dnsCrawlResultResults);
        for (DnsCrawlResult dnsCrawlResultResult : dnsCrawlResultResults) {
            logger.info("dnsCrawlResultResult = {}", dnsCrawlResultResult);
        }
        assertThat(dnsCrawlResultResults.size()).isGreaterThan(0);
    }

    @Test
    public void toParquet() throws IOException {

        logger.info("tempDir = {}", baseLocation);
        Files.createDirectories(baseLocation);
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses();

        logger.info("DnsCrawlResultResult = {}", dnsCrawlResultResult1);

        File jsonFile = tempDir.resolve("dnsCrawlResultResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(dnsCrawlResultResult1));

        repository.storeResults(jsonFile.toString());

        // TODO: implement reading back from parquet
        List<DnsCrawlResult> dnsCrawlResultResults = repository.findByDomainName("example.com");
        List<BaseRepository.SearchVisitIdResultItem> dnsIdAndTimestamp = repository.searchVisitIds("example.com");
        Optional<DnsCrawlResult> dnsCrawlResultLatest = repository.findLatestResult("example.com");
        Optional<DnsCrawlResult> dnsCrwlResultById = repository.findByVisitId("visit-789");

        logger.info("dnsCrawlResultResults found: {}", dnsCrawlResultResults.size());
        logger.info("dnsCrawlResultResults = {}", dnsCrawlResultResults);

        logger.info("ids and timestamps: {}", dnsIdAndTimestamp);
        logger.info("latest dns crawl result: {}", dnsCrawlResultLatest);
        logger.info("dns crawl result by id: {}", dnsCrwlResultById);



        for (DnsCrawlResult dnsCrawlResultResult : dnsCrawlResultResults) {
            logger.info("dnsCrawlResultResult = {}", dnsCrawlResultResult);
        }
        assertThat(dnsCrawlResultResults.size()).isEqualTo(1);
        assertThat(dnsIdAndTimestamp.size()).isEqualTo(1);
        assertThat(dnsIdAndTimestamp.getFirst().getVisitId()).isEqualTo("visit-789");
        assertThat(dnsCrawlResultLatest.isPresent()).isTrue();
        assertThat(dnsCrwlResultById.isPresent()).isTrue();
    }

    @Test
    public void read() throws IOException {
        Resource resource = new ClassPathResource("sql/dns_crawl_results.sql");
        logger.info("classPathResource = {}", resource.getContentAsString(StandardCharsets.UTF_8));
    }

}