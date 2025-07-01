package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.TestUtils;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    public Instant crawlStarted;
    public String visitId;
    public String domainName;

    public BaseItem(Instant crawlStarted, String visitId, String domainName) {
      this.crawlStarted = crawlStarted;
      this.visitId = visitId;
      this.domainName = domainName;
    }
  }

  @Test
  void testAll() throws IOException {
    BaseRepository<BaseItem> repository = new BaseRepository<>(
            TestUtils.jdbcClientFactory(), TestUtils.jsonReader(), baseLocation.toString(), BaseItem.class);

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
    Assertions.assertTrue(byVisitId.isPresent());
    Assertions.assertEquals("visit-2", byVisitId.get().visitId);
    Assertions.assertEquals("domain-name-1", byVisitId.get().domainName);

    // test findByDomainName
    List<BaseItem> byDomainName = repository.findByDomainName("domain-name-1");
    Assertions.assertEquals(2, byDomainName.size());

    // test findLatestResults
    Optional<BaseItem> latestResult = repository.findLatestResult("domain-name-1");
    Assertions.assertTrue(latestResult.isPresent());
    Assertions.assertEquals("visit-2", latestResult.get().visitId);
    Assertions.assertEquals(Instant.ofEpochSecond(2), latestResult.get().crawlStarted);

    // test searchVisitIds
    List<SearchVisitIdResultItem> visitIds = repository.searchVisitIds("domain-name-1");
    assertThat(visitIds.size()).isEqualTo(2);
  }
}