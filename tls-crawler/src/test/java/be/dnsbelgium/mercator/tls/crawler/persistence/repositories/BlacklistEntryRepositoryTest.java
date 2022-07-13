package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.BlacklistEntry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class BlacklistEntryRepositoryTest {

  private static final Logger logger = getLogger(BlacklistEntryRepositoryTest.class);

  @Autowired
  private BlacklistEntryRepository blacklistEntryRepository;

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "tls_crawler");
  }

  @Test
  public void findAll() {
    List<BlacklistEntry> allEntries = blacklistEntryRepository.findAll();
    logger.info("allEntries = {}", allEntries);
    assertThat(allEntries).isEmpty();

    BlacklistEntry entry = new BlacklistEntry("1.3.4.0/24");
    entry = blacklistEntryRepository.save(entry);
    allEntries = blacklistEntryRepository.findAll();
    logger.info("allEntries = {}", allEntries);
    assertThat(allEntries).hasSize(1);
    assertThat(allEntries).containsExactly(entry);
  }

}
