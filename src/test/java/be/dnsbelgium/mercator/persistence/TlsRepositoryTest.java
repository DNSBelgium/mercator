package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TlsRepositoryTest {

  @TempDir
  static Path tempDir;

  private final ObjectMother objectMother = new ObjectMother();
  private TlsRepository repository;

  @Test
  public void saveToParquet() throws IOException {
    // Uncomment to save results in sub-folder of home folder
    // TODO
    Path tempDir = Path.of(System.getProperty("user.home"), "mercator");
    repository = new TlsRepository(tempDir.toAbsolutePath().toString());
    // simulate what the tlsJob does:
    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    File jsonFile = tempDir.resolve("tls.json").toFile();
    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(result1, result2));
    // call class under test
    repository.saveToParquet(jsonFile.toPath(), "tls_output.parquet");
    // todo: add asserts

  }


}