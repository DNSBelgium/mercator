package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.dto.Request;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DnsRepositoryTest {

    private static final Logger logger = LoggerFactory.getLogger(DnsRepositoryTest.class);

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    static Path baseLocation;

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    static Path tempDir;

    private final ObjectMother objectMother = new ObjectMother();
    private final DnsRepository repository = new DnsRepository(TestUtils.jsonReader(), baseLocation.toString());

    @Test
    @EnabledIfEnvironmentVariable(named = "S3_TEST_ENABLED", matches = "True")
    public void toS3Parquet() throws IOException {

        DnsRepository s3DnsRepository = new DnsRepository(TestUtils.jsonReader(), System.getProperty("mercator_s3_base_path"));

        logger.info("tempDir = {}", baseLocation);
        Files.createDirectories(baseLocation);
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses1("dnsbelgium.be", "1");

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
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses1("example.com", "1");

        logger.info("DnsCrawlResultResult = {}", dnsCrawlResultResult1);

        File jsonFile = tempDir.resolve("dnsCrawlResultResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(dnsCrawlResultResult1));

        repository.storeResults(jsonFile.toString());

        List<DnsCrawlResult> dnsCrawlResultResults = repository.findByDomainName("example.com");
        List<SearchVisitIdResultItem> dnsIdAndTimestamp = repository.searchVisitIds("example.com");
        Optional<DnsCrawlResult> dnsCrawlResultLatest = repository.findLatestResult("example.com");
        Optional<DnsCrawlResult> dnsCrwlResultById = repository.findByVisitId("1");

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
        assertThat(dnsIdAndTimestamp.getFirst().getVisitId()).isEqualTo("1");
        assertThat(dnsCrawlResultLatest.isPresent()).isTrue();
        assertThat(dnsCrwlResultById.isPresent()).isTrue();
    }

    @Test
    public void find_whenMultipleDnsCrawlResultsArePresent() throws IOException {
        logger.info("tempDir = {}", baseLocation);
        Files.createDirectories(baseLocation);
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses1("dnsbelgium.be", "1");
        DnsCrawlResult dnsCrawlResultResult2 = objectMother.dnsCrawlResultWithMultipleResponses1("example.be", "2");
        DnsCrawlResult dnsCrawlResultResult3 = objectMother.dnsCrawlResultWithMultipleResponses2("testing.be", "3");

        logger.info("DnsCrawlResultResult = {}", dnsCrawlResultResult1);

        File jsonFile = tempDir.resolve("dnsCrawlResultResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(dnsCrawlResultResult1, dnsCrawlResultResult2, dnsCrawlResultResult3));
        repository.storeResults(jsonFile.toString());

        Optional<DnsCrawlResult> dnsCrwlResultById = repository.findByVisitId("1");
        logger.info("dnsCrwlResultById = {}", dnsCrwlResultById);

        assertThat(dnsCrwlResultById).isPresent();
        assertThat(dnsCrwlResultById.orElseThrow().getVisitId()).isEqualTo("1");
        assertThat(dnsCrwlResultById.get().getRequests().size()).isEqualTo(1);
        Request firstRequest = dnsCrwlResultById.get().getRequests().getFirst();
        assertThat(firstRequest.getResponses().size()).isEqualTo(2);
        assertThat(firstRequest.getResponses().getFirst().getResponseGeoIps().size()).isEqualTo(2);
        // TODO: bram, it seems to be unpredictable which ResponseGeoIp will be first in the list ?
        assertThat(firstRequest.getResponses().getFirst().getResponseGeoIps().getFirst().getIp()).isEqualTo("192.168.1.2");
    }

}