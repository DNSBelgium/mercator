package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TlsRepositoryTest {

  @TempDir
  static Path tempDir;

  private final ObjectMother objectMother = new ObjectMother();
  private TlsRepository repository;
  private static final Logger logger = LoggerFactory.getLogger(TlsRepositoryTest.class);
  private JdbcClient jdbcClient;


  @Test
  @Disabled
  public void saveToParquet() throws IOException {
    // Uncomment to save results in sub-folder of home folder
    // TODO
    //Path tempDir = Path.of(System.getProperty("user.home"), "mercator");
    repository = new TlsRepository(tempDir.toAbsolutePath().toString());
    // simulate what the tlsJob does:
    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    File jsonFile = tempDir.resolve("tls.json").toFile();
    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(result1, result2));
    // call class under test
    // TODO
    // repository.saveToParquet(jsonFile.toPath(), "tls_output.parquet");
    // todo: add asserts
    logger.info("Saved to parquet");
    logger.info(jsonFile.toPath().toString());
    String parquetFilePath  = tempDir.toAbsolutePath().toString() + File.separator + "tls_output.parquet";
    assertThat(Path.of(parquetFilePath).toFile().exists()).isTrue();
    logger.info(parquetFilePath);
  }

  @Test
  @Disabled
  public void getTlsCrawlResultByVisitId() throws IOException {
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    Path tempDir = Path.of(System.getProperty("user.home"), "mercator");
    repository = new TlsRepository(tempDir.toAbsolutePath().toString());
    logger.info(tempDir.toString());
    TlsCrawlResult tlsCrawlResult1 = objectMother.tlsCrawlResult1();

    // TODO: write parquet files here to that path so we can query them in the test with the correct id

    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    File jsonFile = tempDir.resolve("tls.json").toFile();
    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(result1, result2));
    // call class under test
    repository.saveToParquet(jsonFile.toPath(), "tls_output.parquet");

    Optional<TlsCrawlResult> found = repository.findByVisitId("aakjkjkj-ojj");

    logger.info("Found: {}", found );
    assertTrue(found.isPresent());
    assertEquals(tlsCrawlResult1, found.get());






  }


}