package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpVisitRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class SmtpVisitRepositoryTest {
  @Autowired
  SmtpVisitRepository repository;
  @Autowired
  ApplicationContext context;
  @Autowired
  JdbcTemplate jdbcTemplate;

  private static final Logger logger = getLogger(SmtpCrawlResultRepositoryTest.class);

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void addVisitTest(){
    UUID visitId = UUID.randomUUID();
    ZonedDateTime timestamp = ZonedDateTime.now();
    SmtpVisitEntity visit = new SmtpVisitEntity();
    visit.setVisitId(visitId);
    visit.setDomainName("dnsbelgium.be");
    visit.setTimestamp(timestamp);
    visit.setNumConversations(0);
    repository.save(visit);
    Optional<SmtpVisitEntity> savedVisit = repository.findByVisitId(visitId);
    Assertions.assertThat(savedVisit).isPresent();
    SmtpVisitEntity visitEntity = savedVisit.get();
    assertThat(visitEntity.getVisitId()).isEqualTo(visitId);
    assertThat(visitEntity.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(visitEntity.getTimestamp()).isEqualTo(timestamp);
    assertThat(visitEntity.getNumConversations()).isEqualTo(0);
  }
}
