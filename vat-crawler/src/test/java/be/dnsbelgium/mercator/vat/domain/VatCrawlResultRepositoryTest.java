package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.vat.crawler.persistence.VatCrawlResult;
import be.dnsbelgium.mercator.vat.crawler.persistence.VatCrawlResultRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class VatCrawlResultRepositoryTest {

  @Autowired
  private VatCrawlResultRepository vatCrawlResultRepository;

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  private static final Logger logger = getLogger(VatCrawlResultRepositoryTest.class);

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "vat_crawler");
  }

  @Test
  @Commit
  public void insert() {
    UUID visitId = UUID.randomUUID();
    VatCrawlResult vatCrawlResult = new VatCrawlResult();

    vatCrawlResult.setDomainName("dnsbelgium.be");
    vatCrawlResult.setVisitId(visitId);

    vatCrawlResult.setVatValues(List.of("BE-0466158640", "BE-0123455"));

    vatCrawlResult.setCrawlStarted(Instant.now());
    vatCrawlResult.setCrawlFinished(Instant.now().plusSeconds(123));

    vatCrawlResult.setVisitedUrls(List.of(
        "https://www.dnsbelgium.be/",
        "https://www.dnsbelgium.be/nl/over-dns-belgium",
        "https://www.dnsbelgium.be/nl/contact"));

    vatCrawlResultRepository.save(vatCrawlResult);
    logger.info("vatCrawlResult = {}", vatCrawlResult);

    Optional<VatCrawlResult> found = vatCrawlResultRepository.findById(vatCrawlResult.getId());
    logger.info("found = {}", found);


  }
}
