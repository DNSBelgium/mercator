package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Disabled;
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

class WebRepositoryTest {

  private static final Logger logger = LoggerFactory.getLogger(WebRepositoryTest.class);

  // you can temporarily change this to NEVER if you want to inspect the files
  @TempDir(cleanup = CleanupMode.ALWAYS)
  static Path baseLocation;

  @TempDir(cleanup = CleanupMode.ALWAYS)
  static Path tempDir;

  private final ObjectMother objectMother = new ObjectMother();

  private final WebRepository repository = makeRepository(baseLocation.toString());

  private WebRepository makeRepository(String baseLocation) {
    return new WebRepository(TestUtils.jdbcClient(), TestUtils.jsonReader(), baseLocation);
  }


  @Test
  @EnabledIfEnvironmentVariable(named = "S3_TEST_ENABLED", matches = "true")
  public void toS3Parquet() throws IOException {
    WebRepository s3WebRepository = makeRepository(System.getProperty("mercator_s3_base_path"));
    logger.info("tempDir = {}", baseLocation);
    Files.createDirectories(baseLocation);
    WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
    WebCrawlResult webCrawlResult2 = objectMother.webCrawlResult2();

    logger.info("webCrawlResult1 = {}", webCrawlResult1);
    logger.info("webCrawlResult2 = {}", webCrawlResult2);

    File jsonFile = tempDir.resolve("webCrawlResult1.json").toFile();
    logger.info("jsonFile = {}", jsonFile);

    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(webCrawlResult1, webCrawlResult2));

    s3WebRepository.storeResults(jsonFile.toString());

    List<WebCrawlResult> webCrawlResults = s3WebRepository.findByDomainName("dnsbelgium.be");
    logger.info("webCrawlResults found: {}", webCrawlResults.size());
    logger.info("webCrawlResults = {}", webCrawlResults);
    for (WebCrawlResult webCrawlResult : webCrawlResults) {
      logger.info("webCrawlResult = {}", webCrawlResult);
    }
    assertThat(webCrawlResults.size()).isGreaterThan(0);
  }

  @Test
  public void toParquet() throws IOException {
    logger.info("tempDir = {}", baseLocation);
    Files.createDirectories(baseLocation);
    WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
    WebCrawlResult webCrawlResult2 = objectMother.webCrawlResult2();

    WebRepository repository = makeRepository(baseLocation.toString());

    logger.info("webCrawlResult1 = {}", webCrawlResult1);
    logger.info("webCrawlResult2 = {}", webCrawlResult2);

    File jsonFile = tempDir.resolve("*.json").toFile();
    logger.info("jsonFile = {}", jsonFile);

    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(webCrawlResult1, webCrawlResult2));

    repository.storeResults(jsonFile.toString());

    List<WebCrawlResult> webCrawlResults = repository.findByDomainName("dnsbelgium.be");
    logger.info("webCrawlResults found: {}", webCrawlResults.size());
    logger.info("webCrawlResults = {}", webCrawlResults);
    for (WebCrawlResult webCrawlResult : webCrawlResults) {
      logger.info("webCrawlResult = {}", webCrawlResult);
    }
    assertThat(webCrawlResults.size()).isEqualTo(1);
    assertThat(webCrawlResults.getFirst())
            .usingRecursiveComparison()
            .isEqualTo(webCrawlResult1);
  }


}