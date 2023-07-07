package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpVisitRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
class SmtpVisitRepositoryTest {

  @Autowired
  SmtpVisitRepository repository;
  @Autowired
  SmtpHostRepository smtpHostRepository;
  @Autowired
  SmtpConversationRepository conversationRepository;
  @Autowired
  ApplicationContext context;
  @Autowired
  JdbcTemplate jdbcTemplate;

  private static final Logger logger = getLogger(SmtpVisitRepositoryTest.class);

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
    SmtpVisit visit = new SmtpVisit();
    visit.setVisitId(visitId);
    visit.setDomainName("dnsbelgium.be");
    visit.setTimestamp(timestamp);
    visit.setNumConversations(0);
    repository.save(visit);
    Optional<SmtpVisit> savedVisit = repository.findByVisitId(visitId);
    org.assertj.core.api.Assertions.assertThat(savedVisit).isPresent();
    SmtpVisit saved = savedVisit.get();
    assertThat(saved.getVisitId()).isEqualTo(visitId);
    assertThat(saved.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(saved.getTimestamp()).isEqualTo(timestamp);
    assertThat(saved.getNumConversations()).isEqualTo(0);
  }

  @Test
  void findByVisitId() {
    UUID uuid = randomUUID();
    logger.info("uuid = {}", uuid);
    SmtpVisit visit = new SmtpVisit(uuid, "dnsbelgium.be");
    SmtpHost host1 = new SmtpHost("smtp1.example.com");
    SmtpHost host2 = new SmtpHost("smtp2.example.com");
    var conversation = new SmtpConversation();
    conversation.setIp("1.2.3.4");
    conversation.setConnectReplyCode(220);
    conversation.setIpVersion(4);
    conversation.setBanner("my banner");
    conversation.setConnectionTimeMs(123);
    conversation.setStartTlsOk(false);
    conversation.setCountry("Jamaica");
    conversation.setAsnOrganisation("Happy Green grass");
    conversation.setAsn(654L);
    host1.setConversation(conversation);
    host2.setConversation(conversation);
    visit.add(host1);
    visit.add(host2);
    conversationRepository.save(conversation);

    repository.save(visit);
    // null value in column "ip" violates not-null constraint ??

    Optional<SmtpVisit> found = repository.findByVisitId(uuid);
    assertThat(found).isPresent();
    SmtpVisit foundVisit = found.get();
    logger.info("found.servers = {}", foundVisit.getHosts());
    assertThat(foundVisit).isNotNull();
    assertThat(foundVisit.getTimestamp()).isEqualTo(visit.getTimestamp());
    assertThat(foundVisit.getDomainName()).isEqualTo(visit.getDomainName());
    assertThat(foundVisit.getVisitId()).isEqualTo(visit.getVisitId());
    assertThat(foundVisit.getHosts().size()).isEqualTo(2);
    assertThat(foundVisit.getHosts().get(0).getHostName()).isEqualTo(visit.getHosts().get(0).getHostName());

    SmtpConversation conversation1 = foundVisit.getHosts().get(0).getConversation();
    assertThat(conversation1).isNotNull();
    assertThat(conversation1.getIp()).isEqualTo("1.2.3.4");
    assertThat(conversation1.getBanner()).isEqualTo("my banner");
  }

  @Test
  public void savingBinaryDataFails() {
    SmtpVisit visit = smtpVisitWithBinaryData();
    try {
      for (var host : visit.getHosts()) {
        conversationRepository.save(host.getConversation());
      }
      repository.save(visit);

      fail("Binary data should throw DataIntegrityViolationException");
    } catch (DataIntegrityViolationException expected) {
      logger.info("expected = {}", expected.getMessage());
    }
  }


  @Test
  public void save() {
    var uuid = UUID.randomUUID();
    SmtpVisit visit = new SmtpVisit(uuid, "test.be");
    SmtpHost host = new SmtpHost("mx.test.be");
    visit.add(host);
    var saved = repository.save(visit);
    logger.info("saved = {}", saved);
  }

  @Test
  public void saveSuccessfulWhenWeCleanBinaryData() {
    SmtpVisit smtpVisit = smtpVisitWithBinaryData();
    // clean the data before saving
    for (SmtpHost host : smtpVisit.getHosts()) {
        host.getConversation().clean();
    }
    String actualCountry = smtpVisit.getHosts().get(0).getConversation().getCountry();
    assertThat(actualCountry).isEqualTo("Jamaica ");
    logger.info("Before save: smtpVisit.getVisitId() = {}", smtpVisit.getVisitId());
    smtpVisit = repository.save(smtpVisit);
    logger.info("After save: crawlResult.getVisitId() = {}", smtpVisit.getVisitId());
    assertThat(smtpVisit.getVisitId()).isNotNull();
  }

  private SmtpVisit smtpVisitWithBinaryData() {
    UUID uuid = randomUUID();
    SmtpVisit visit = new SmtpVisit(uuid, "dnsbelgium.be");
    SmtpHost host = new SmtpHost("smtp1.example.com");
    var conversation = new SmtpConversation();
    conversation.setIp("1.2.3.4");
    conversation.setConnectReplyCode(220);
    conversation.setIpVersion(4);
    conversation.setBanner("my binary \u0000 banner");
    conversation.setConnectionTimeMs(123);
    conversation.setStartTlsOk(false);
    conversation.setCountry("Jamaica \u0000");
    conversation.setAsnOrganisation("Happy \u0000 Green grass");
    conversation.setAsn(654L);
    host.setConversation(conversation);
    visit.add(host);
    return visit;
  }

}
