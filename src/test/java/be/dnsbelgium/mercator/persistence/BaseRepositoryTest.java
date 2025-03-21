package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.TestUtils;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class BaseRepositoryTest {

  @TempDir
  static Path tempDir;

  @TempDir
  static Path baseLocation;

  static ObjectWriter objectWriter = TestUtils.jsonWriter();

  /**
   * Sample class to store and find
   */
  public static class BaseItem {
    public Instant crawlTimestamp;
    public String visitId;
    public String domainName;

    public BaseItem(Instant crawlTimestamp, String visitId, String domainName) {
      this.crawlTimestamp = crawlTimestamp;
      this.visitId = visitId;
      this.domainName = domainName;
    }
  }

  @Test
  void testAll() throws IOException {

    BaseRepository<BaseItem> repository = new BaseRepository<>(TestUtils.jsonReader(), baseLocation.toString(), BaseItem.class);

    // prep data
    File jsonFile = tempDir.resolve("data.json").toFile();
    List<BaseItem> baseItems = List.of(new BaseItem(Instant.ofEpochSecond(1), "visit-1", "domain-name-1"),
                                       new BaseItem(Instant.ofEpochSecond(2), "visit-2", "domain-name-1"),
                                       new BaseItem(Instant.ofEpochSecond(3), "visit-3", "domain-name-2"));
    objectWriter.writeValue(jsonFile, baseItems);

    // save data in repository
    repository.storeResults(jsonFile.toString());

    // test findByVisitId
    Optional<BaseItem> byVisitId = repository.findByVisitId("visit-2");
    assertTrue(byVisitId.isPresent());
    assertEquals("visit-2", byVisitId.get().visitId);
    assertEquals("domain-name-1", byVisitId.get().domainName);

    // test findByDomainName
    List<BaseItem> byDomainName = repository.findByDomainName("domain-name-1");
    assertEquals(2, byDomainName.size());

    // test findLatestResults
    Optional<BaseItem> latestResult = repository.findLatestResult("domain-name-1");
    assertTrue(latestResult.isPresent());
    assertEquals("visit-2", latestResult.get().visitId);
    assertEquals(Instant.ofEpochSecond(2), latestResult.get().crawlTimestamp);

    // test searchVisitIds
    List<BaseRepository.SearchVisitIdResultItem> visitIds = repository.searchVisitIds("domain-name-1");
    assertEquals(2, visitIds.size());
  }
}