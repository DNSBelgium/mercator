package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TlsRepositoryTest {

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path tempDir;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path baseLocation;

  private final ObjectMother objectMother = new ObjectMother();
  private static final Logger logger = LoggerFactory.getLogger(TlsRepositoryTest.class);

  @Test
  public void pojo_to_parquet_and_back() throws IOException {
    TlsRepository repository = makeRepository();
    TlsCrawlResult input = objectMother.tlsCrawlResult2();
    String path = saveToJson(List.of(input), "tls.json");
    repository.storeResults(path);
    TlsCrawlResult output = repository.findByVisitId(input.getVisitId()).orElseThrow();
    assertThat(output)
            .usingRecursiveComparison()
            .isEqualTo(input);
  }

  @Test
  public void testAll() throws IOException {
    TlsRepository repository = makeRepository();
    // simulate what the tlsJob does:
    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    TlsCrawlResult result3 = objectMother.tlsCrawlResult3();

    String jsonLocation = saveToJson(List.of(result1, result2, result3), "tls-data.json");
    repository.storeResults(jsonLocation);

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

  private TlsRepository makeRepository() {
    return new TlsRepository(TestUtils.jsonReader(), baseLocation.toString());
  }

  private String saveToJson(List<TlsCrawlResult> crawlResults, String fileName) throws IOException {
    File jsonFile = tempDir.resolve(fileName).toFile();
    TestUtils.jsonWriter().writeValue(jsonFile, crawlResults);
    String json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
    logger.info("json = \n {}", json);
    String jsonLocation = jsonFile.getAbsolutePath();
    logger.info("jsonLocation = {}", jsonLocation);
    return jsonLocation;
  }

}