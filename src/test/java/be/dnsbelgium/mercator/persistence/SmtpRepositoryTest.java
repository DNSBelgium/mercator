package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

        List<SmtpVisit> smtpVisitResults = repository.findByDomainName("example.com");
        List<BaseRepository.SearchVisitIdResultItem> smtpIdAndTimestamp = repository.searchVisitIds("example.com");
        Optional<SmtpVisit> smtpVisitResultByLatest = repository.findLatestResult("example.com");
        Optional<SmtpVisit> smtpVisitResultById = repository.findByVisitId("01HJR2Z6DZHS4G4P9X1BZSD4YV");

        logger.info("smtpVisitResults found: {}", smtpVisitResults.size());
        logger.info("smtpVisitResults = {}", smtpVisitResults);
        logger.info("smtpconversation = {}", smtpVisitResults.getFirst().getHosts().get(0).getConversation());
        logger.info("smtpconversation = {}", smtpVisitResults.getFirst().getHosts().get(1).getConversation());

        logger.info("ids and timestamp for smtp visit results: {}", smtpIdAndTimestamp.getFirst().getVisitId() + ":" + smtpIdAndTimestamp.get(0).getTimestamp());
        logger.info("latest smtp visit results: {}", smtpVisitResultByLatest);
        logger.info("smtp visit by id: {}", smtpVisitResultById.get().getVisitId());

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

}