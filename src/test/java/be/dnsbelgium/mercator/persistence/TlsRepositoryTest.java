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

  @TempDir
  static Path baseLocation;

  private final ObjectMother objectMother = new ObjectMother();
  private TlsRepository repository;
  private static final Logger logger = LoggerFactory.getLogger(TlsRepositoryTest.class);


  @Test
  @Disabled
  public void findByDomainName() throws IOException {
    repository = new TlsRepository(TestUtils.jsonReader(), baseLocation.toString());
    // simulate what the tlsJob does:
    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    File jsonFile = tempDir.resolve("tls.json").toFile();
    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(result1, result2));


    repository.storeResults(jsonFile.toString());
    List<TlsCrawlResult> byDomainName = repository.findByDomainName("example.org");

    assertEquals(2, byDomainName.size());
    assertEquals(List.of("example.org"), byDomainName.stream().map(r -> r.getVisitRequest().getDomainName()).toList());

  }

  @Test
  @Disabled
  public void findLatestResult() throws IOException {
    repository = new TlsRepository(TestUtils.jsonReader(), baseLocation.toString());
    logger.info(tempDir.toString());
    TlsCrawlResult tlsCrawlResult1 = objectMother.tlsCrawlResult1();

    // TODO: write parquet files here to that path so we can query them in the test with the correct id

    TlsCrawlResult result1 = objectMother.tlsCrawlResult1();
    TlsCrawlResult result2 = objectMother.tlsCrawlResult2();
    File jsonFile = tempDir.resolve("tls.json").toFile();
    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(result1, result2));
    // call class under test
    repository.storeResults(jsonFile.toString());

    Optional<TlsCrawlResult> found = repository.findLatestResult("example.org");

    logger.info("Found: {}", found);
    assertTrue(found.isPresent());
    assertEquals("example.org", found.get().getVisitRequest().getDomainName());

  }


}