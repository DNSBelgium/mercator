package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class WebRepositoryTest {

  private static final Logger logger = LoggerFactory.getLogger(WebRepositoryTest.class);

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
  private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
  private final WebRepository repository = new WebRepository(TestUtils.jsonReader(), baseLocation.toString(), JdbcClient.create(DuckDataSource.memory()));



  @Test
  @EnabledIfEnvironmentVariable(named = "S3_TEST_ENABLED", matches = "True")
  public void toS3Parquet() throws IOException {

    WebRepository s3WebRepository = new WebRepository(TestUtils.jsonReader(), System.getProperty("mercator_s3_base_path"), JdbcClient.create(DuckDataSource.memory()));

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

    logger.info("webCrawlResult1 = {}", webCrawlResult1);
    logger.info("webCrawlResult2 = {}", webCrawlResult2);

    File jsonFile = tempDir.resolve("webCrawlResult1.json").toFile();
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