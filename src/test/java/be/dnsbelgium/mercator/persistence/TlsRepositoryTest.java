package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TlsRepositoryTest {

  @TempDir(cleanup = CleanupMode.NEVER)
  static Path tempDir;

  @TempDir(cleanup = CleanupMode.NEVER)
  static Path baseLocation;

  private final ObjectMother objectMother = new ObjectMother();
  private static final Logger logger = LoggerFactory.getLogger(TlsRepositoryTest.class);

  @Test
  public void testAll() throws IOException {
    TlsRepository repository = new TlsRepository(TestUtils.jsonReader(), baseLocation.toString());
    // simulate what the tlsJob does:
    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    TlsCrawlResult result3 = objectMother.tlsCrawlResult3();
    File jsonFile = tempDir.resolve("tls.json").toFile();
    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(result1, result2, result3));
    String jsonLocation = jsonFile.getAbsolutePath();
    logger.info("jsonLocation = {}", jsonLocation);
    repository.storeResults(jsonLocation);
    String json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
    logger.info("json = \n\n {}", json);

    logger.info("jsonWriter = {}", jsonWriter);

    List<TlsCrawlResult> byDomainName = repository.findByDomainName("example.be");

    assertEquals(2, byDomainName.size());
    assertEquals(Set.of("example.be"), byDomainName.stream().map(TlsCrawlResult::getDomainName).collect(Collectors.toSet()));

    Optional<TlsCrawlResult> byVisitId = repository.findByVisitId("visit01");
    assertTrue(byVisitId.isPresent());
    assertEquals("visit01", byVisitId.get().getVisitId());

    List<SearchVisitIdResultItem> items = repository.searchVisitIds("example.be");
    assertEquals(2, items.size());

    Optional<TlsCrawlResult> latestResult = repository.findLatestResult("example.be");
    assertTrue(latestResult.isPresent());
  }

}