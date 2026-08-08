package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
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

class WebRepositoryTest {

  private static final Logger logger = LoggerFactory.getLogger(WebRepositoryTest.class);

  // you can temporarily change this to NEVER if you want to inspect the files
  @TempDir(cleanup = CleanupMode.ALWAYS)
  static Path baseLocation;

  @TempDir(cleanup = CleanupMode.ALWAYS)
  static Path tempDir;

  private final ObjectMother objectMother = new ObjectMother();

  private WebRepository makeRepository() {
    String webLocation = baseLocation.resolve("web").toString();
    String responseBodyLocation = baseLocation.resolve("response_body").toString();
    return makeRepository(webLocation, responseBodyLocation);
  }

  private WebRepository makeRepository(String loc1, String loc2) {
    return new WebRepository(TestUtils.jdbcClientFactory(), TestUtils.jsonReader(), loc1, loc2);
  }


  @Test
  @EnabledIfEnvironmentVariable(named = "S3_TEST_ENABLED", matches = "true")
  public void toS3Parquet() throws IOException {
    String webLocation = System.getProperty("mercator_s3_base_path") + "/web";
    String responseBodyLocation = System.getProperty("mercator_s3_base_path") + "/response_body";
    WebRepository s3WebRepository = makeRepository(webLocation, responseBodyLocation);
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

    WebRepository repository = makeRepository();

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
            .ignoringFields("pageVisits.responseBody")
            .isEqualTo(webCrawlResult1);

    // assert that responseBody was written to parquet files
    String finalUrl = webCrawlResult1.getPageVisits().getFirst().getFinalUrl();

    Optional<String> responseBody = repository.getResponseBody(webCrawlResult1.year(), webCrawlResult1.month(), webCrawlResult1.getVisitId(), finalUrl);
    logger.info("responseBody = {}", responseBody);
    assertThat(responseBody).isPresent();
    responseBody.ifPresent(s -> assertThat(s).isEqualTo(webCrawlResult1.getPageVisits().getFirst().getResponseBody()));
  }

}