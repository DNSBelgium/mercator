package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

class WebRepositoryTest {

  @TempDir
  static Path tempDir;

  //@TempDir
  //static Path tempDir = Path.of(System.getProperty("user.home"), "mercator");

  private static final Logger logger = LoggerFactory.getLogger(WebRepositoryTest.class);
  ObjectMother objectMother = new ObjectMother();

  @Test
  public void toParquet() throws IOException {
    logger.info("tempDir = {}", tempDir);
    WebRepository repository = new WebRepository(tempDir.toAbsolutePath().toString());

    WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
    WebCrawlResult webCrawlResult2 = objectMother.webCrawlResult2();

    File jsonFile = tempDir.resolve("webCrawlResult1.json").toFile();

    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(webCrawlResult1, webCrawlResult2));

    repository.saveToParquet(jsonFile.toPath(), "WebRepositoryTest_test1.parquet");
    // todo: add asserts
  }

}