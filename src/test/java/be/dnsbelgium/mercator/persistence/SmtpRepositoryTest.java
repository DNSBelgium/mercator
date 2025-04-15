package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
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

class SmtpRepositoryTest {

    private static final Logger logger = LoggerFactory.getLogger(SmtpRepositoryTest.class);

    @TempDir(cleanup = CleanupMode.NEVER)
    static Path baseLocation;

    @TempDir(cleanup = CleanupMode.NEVER)
    static Path tempDir;

    private final ObjectMother objectMother = new ObjectMother();
    private final SmtpRepository repository = new SmtpRepository(TestUtils.jsonReader(), baseLocation.toString());

    @Test
    @EnabledIfEnvironmentVariable(named = "S3_TEST_ENABLED", matches = "True")
    public void toS3Parquet() throws IOException {

        SmtpRepository s3SmtpRepository = new SmtpRepository(TestUtils.jsonReader(), System.getProperty("mercator_s3_base_path"));

        logger.info("tempDir = {}", baseLocation);
        Files.createDirectories(baseLocation);
        SmtpVisit smtpVisitResult1 = objectMother.smtpVisit1();

        logger.info("SmtpVisitResult = {}", smtpVisitResult1);

        File jsonFile = tempDir.resolve("smtpVisitResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(smtpVisitResult1));

        s3SmtpRepository.storeResults(jsonFile.toString());

        List<SmtpVisit> smtpVisitResults = s3SmtpRepository.findByDomainName("example.com");
        logger.info("smtpVisitResults found: {}", smtpVisitResults.size());
        logger.info("smtpVisitResults = {}", smtpVisitResults);
        for (SmtpVisit smtpVisitResult : smtpVisitResults) {
            logger.info("smtpVisitResult = {}", smtpVisitResult);
        }
        assertThat(smtpVisitResults.size()).isGreaterThan(0);
    }

    @Test
    public void toParquet() throws IOException {

        logger.info("tempDir = {}", baseLocation);
        Files.createDirectories(baseLocation);
        SmtpVisit smtpVisitResult1 = objectMother.smtpVisit1();

        logger.info("SmtpVisitResult = {}", smtpVisitResult1);

        File jsonFile = tempDir.resolve("smtpVisitResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(smtpVisitResult1));

        repository.storeResults(jsonFile.toString());

        List<SmtpVisit> smtpVisitResults = repository.findByDomainName("example1.com");
        List<SearchVisitIdResultItem> smtpIdAndTimestamp = repository.searchVisitIds("example1.com");
        Optional<SmtpVisit> smtpVisitResultByLatest = repository.findLatestResult("example1.com");
        Optional<SmtpVisit> smtpVisitResultById = repository.findByVisitId("01HJR2Z6DZHS4G4P9X1BZSD4YV");

        logger.info("smtpVisitResults found: {}", smtpVisitResults.size());
        logger.info("smtpVisitResults = {}", smtpVisitResults);
        logger.info("smtpconversation = {}", smtpVisitResults.getFirst().getHosts().get(0).getConversation());
        logger.info("smtpconversation = {}", smtpVisitResults.getFirst().getHosts().get(1).getConversation());

        SearchVisitIdResultItem first = smtpIdAndTimestamp.getFirst();
        logger.info("ids and timestamp for smtp visit results: {}", first.getVisitId() + ":" + first.getTimestamp());
        logger.info("latest smtp visit results: {}", smtpVisitResultByLatest);
        logger.info("smtp visit by id: {}", smtpVisitResultById.orElseThrow().getVisitId());

        for (SmtpVisit smtpVisitResult : smtpVisitResults) {
            logger.info("smtpVisitResult = {}", smtpVisitResult);
        }
        assertThat(smtpVisitResults.size()).isEqualTo(1);
        assertThat(smtpVisitResults.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(smtpVisitResult1);
        assertThat(smtpVisitResults.getFirst().getHosts().size()).isEqualTo(2);
        assertThat(smtpVisitResults.getFirst().getHosts().get(0).getConversation().toString()).isNotEmpty();
        assertThat(smtpVisitResults.getFirst().getHosts().get(1).getConversation().toString()).isNotEmpty();
        logger.info(smtpVisitResults.getFirst().getHosts().get(0).getConversation().getTimestamp().toString());
        logger.info(smtpVisitResults.getFirst().getTimestamp().toString());
    }

    @Test
    public void findShouldReturnCorrectSmtpVisitResultsWhenMultipleWithMultipleHostsArePresent() throws IOException {
        SmtpVisit smtpVisitResult1 = objectMother.smtpVisit1();
        SmtpVisit smtpVisitResult2 = objectMother.smtpVisit2();

        File jsonFile = tempDir.resolve("smtpVisitResult1.json").toFile();
        logger.info("jsonFile = {}", jsonFile);

        ObjectWriter jsonWriter = TestUtils.jsonWriter();
        jsonWriter.writeValue(jsonFile, List.of(smtpVisitResult1, smtpVisitResult2));

        repository.storeResults(jsonFile.toString());

        // when
        List<SmtpVisit> smtpVisitResults1 = repository.findByDomainName("example1.com");
        List<SmtpVisit> smtpVisitResults2 = repository.findByDomainName("example2.com");

        // then
        assertThat(smtpVisitResults1.size()).isEqualTo(1);
        assertThat(smtpVisitResults2.size()).isEqualTo(1);

        List<String> hostIdsVisitResultts1 = new ArrayList<>();

        for (SmtpHost smtpHost : smtpVisitResults1.getFirst().getHosts()) {
            hostIdsVisitResultts1.add(smtpHost.getId());

        }

        logger.info("ids found (there should be 2): {}",hostIdsVisitResultts1);
        assertThat(hostIdsVisitResultts1.size()).isEqualTo(2);
        assertThat(smtpVisitResults1.getFirst().getHosts().size()).isEqualTo(2);
        assertThat(smtpVisitResults2.getFirst().getHosts().size()).isEqualTo(2);

        assertThat(smtpVisitResults1.getFirst().getHosts().getFirst().getConversation().toString()).isNotEmpty();
        assertThat(smtpVisitResults2.getFirst().getHosts().getFirst().getConversation().toString()).isNotEmpty();






    }

}