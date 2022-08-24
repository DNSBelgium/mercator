package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;

@ActiveProfiles({"local", "test"})
@SpringJUnitConfig({S3Config.class, HtmlReader.class})
class HtmlReaderTest {

  @Container static LocalstackContainer localstack = new LocalstackContainer();
  @Autowired private HtmlReader htmlReader;
  @Autowired private AmazonS3 amazonS3;
  private static final String HTML_KEY = "some/key/on/s3";
  private static final String HTML_CONTENT = "<html><body>Hello world</body></html>";
  private static final String BUCKET_NAME = "some-s3-bucket-name";
  private static final Logger logger = getLogger(HtmlReaderTest.class);

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    localstack.setDynamicPropertySource(registry);
    String s3 = localstack.getEndpointOverride(LocalStackContainer.Service.S3).toASCIIString();
    logger.info("s3,endpoint = {}", s3);
  }

  @BeforeEach
  public void setup() {
    Bucket bucket = amazonS3.createBucket(BUCKET_NAME);
    logger.debug("Bucket created: {}", bucket);
    amazonS3.putObject(BUCKET_NAME, HTML_KEY, HTML_CONTENT);
    logger.debug("Object created: {}", HTML_KEY);
  }

  @Test
  public void read() throws IOException {
    ContentCrawlResult crawlResult = new ContentCrawlResult(UUID.randomUUID(), "abc.be", "http://abc.be", true, null, 0);
    crawlResult.setHtmlKey(HTML_KEY);
    crawlResult.setBucket(BUCKET_NAME);
    InputStream inputStream = htmlReader.read(crawlResult);
    StringWriter writer = new StringWriter();
    FileCopyUtils.copy(new InputStreamReader(inputStream), writer);
    logger.info("writer = {}", writer.toString());
    assertThat(writer.toString()).isEqualTo(HTML_CONTENT);
  }

  @Test
  public void readNonExistingKey() {
    ContentCrawlResult crawlResult = new ContentCrawlResult(UUID.randomUUID(), "abc.be", "http://abc.be", true, null, 0);
    crawlResult.setHtmlKey("THIS-KEY_DOES-NOT-EXIST");
    crawlResult.setBucket(BUCKET_NAME);
    Exception exception = assertThrows(AmazonS3Exception.class, () -> htmlReader.read(crawlResult));
    logger.debug("OK, htmlReader.read() threw {}", exception.toString());
  }

  @Test
  public void readFromWrongBucket() {
    ContentCrawlResult crawlResult = new ContentCrawlResult(UUID.randomUUID(), "abc.be", "http://abc.be", true, null, 0);
    crawlResult.setHtmlKey(HTML_KEY);
    crawlResult.setBucket("THIS-BUCKET_DOES-NOT-EXIST");
    Exception exception = assertThrows(AmazonS3Exception.class, () -> htmlReader.read(crawlResult));
    logger.debug("OK, htmlReader.read() threw {}", exception.toString());
  }

}
