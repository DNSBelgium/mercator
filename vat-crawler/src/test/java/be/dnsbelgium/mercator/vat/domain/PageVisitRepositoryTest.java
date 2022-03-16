package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisit;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisitRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class PageVisitRepositoryTest {

  @Autowired
  private PageVisitRepository pageVisitRepository;

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
    PageVisit pageVisit = new PageVisit(
        visitId,
        "dnsbelgium.be",
        "http://www.dnsbelgium.be/contact",
        "/contact",
        Instant.now(),
        Instant.now().plusSeconds(123),
        200,
        "Wow, fancy website!",
        List.of("BE-0466158640", "BE-0123455")
    );
    pageVisitRepository.save(pageVisit);
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getId()).isNotNull();
    Optional<PageVisit> found = pageVisitRepository.findById(pageVisit.getId());
    assertThat(found).isPresent();
  }

  @Test
  public void save_0x00() {
    UUID visitId = UUID.randomUUID();
    PageVisit pageVisit = new PageVisit(
        visitId,
        "just-a-test.be",
        "http://www.just-a-test.be/null-bytes",
        "/null-bytes-\u0000",
        Instant.now(),
        Instant.now().plusSeconds(123),
        200,
        "Wow, binary data with \u0000 bytes",
        List.of()
    );
    logger.info("This should NOT give a DataIntegrityViolationException");
    pageVisitRepository.save(pageVisit);
    logger.info("pageVisit = {}", pageVisit);
  }

}
