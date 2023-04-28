package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
class SmtpCrawlResultRepositoryTest {

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

  //TODO refactor

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
    org.assertj.core.api.Assertions.assertThat(savedVisit).isPresent();
    SmtpVisitEntity visitEntity = savedVisit.get();
    assertThat(visitEntity.getVisitId()).isEqualTo(visitId);
    assertThat(visitEntity.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(visitEntity.getTimestamp()).isEqualTo(timestamp);
    assertThat(visitEntity.getNumConversations()).isEqualTo(0);
  }

  @Test
  void findByVisitId() {
    UUID uuid = randomUUID();
    logger.info("uuid = {}", uuid);
    SmtpVisitEntity visitEntity = new SmtpVisitEntity(uuid, "dnsbelgium.be");
    SmtpHostEntity host1 = new SmtpHostEntity("smtp1.example.com");
    SmtpHostEntity host2 = new SmtpHostEntity("smtp2.example.com");
    SmtpConversation conversation = new SmtpConversation("1.2.3.4");
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
    visitEntity.add(host1);
    visitEntity.add(host2);
    repository.save(visitEntity);
    Optional<SmtpVisitEntity> found = repository.findByVisitId(uuid);
    assertThat(found).isPresent();
    SmtpVisitEntity foundVisit = found.get();
    logger.info("found.servers = {}", foundVisit.getHosts());
    assertThat(foundVisit).isNotNull();
    assertThat(foundVisit.getTimestamp()).isEqualTo(visitEntity.getTimestamp());
    assertThat(foundVisit.getDomainName()).isEqualTo(visitEntity.getDomainName());
    assertThat(foundVisit.getVisitId()).isEqualTo(visitEntity.getVisitId());
    assertThat(foundVisit.getHosts().size()).isEqualTo(2);
    assertThat(foundVisit.getHosts().get(0).getHostName()).isEqualTo(visitEntity.getHosts().get(0).getHostName());

    SmtpConversationEntity conversation1 = foundVisit.getHosts().get(0).getConversation();
    assertThat(conversation1).isNotNull();
    assertThat(conversation1.getIp()).isEqualTo("1.2.3.4");
    assertThat(conversation1.getBanner()).isEqualTo("my banner");
  }

  @Test
  public void savingBinaryDataFails() {
    SmtpVisitEntity visitEntity = smtpVisitWithBinaryData();
    try {
      //TODO potentieel fix nodig
      SmtpVisitEntity visit = repository.save(visitEntity);
      //noinspection ResultOfMethodCallIgnored
      fail("Binary data should throw DataIntegrityViolationException");
    } catch (DataIntegrityViolationException expected) {
      logger.info("expected = {}", expected.getMessage());
    }
  }


  @Test
  public void saveAndIgnoreDuplicateKeys() {
    UUID uuid = randomUUID();
    @SuppressWarnings("SqlResolve")
    int rowsInserted = jdbcTemplate.update("" +
        " insert into smtp_visit\n" +
        "        (timestamp, domain_name, visit_id, num_conversations) \n" +
        "    values\n" +
        "        (current_timestamp, ?, ?, ?)"
      , "abc.be", uuid, 0
    );
    logger.info("rowsInserted = {}", rowsInserted);
    Optional<SmtpVisitEntity> found = repository.findByVisitId(uuid);
    logger.info("found = {}", found);
    assertThat(found).isPresent();
    jdbcTemplate.execute("commit");
    SmtpVisitEntity crawlResult = smtpVisit(uuid);
    boolean saveFailed = repository.saveAndIgnoreDuplicateKeys(crawlResult).isPresent();
    logger.info("saveFailed = {}", saveFailed);
    assertThat(saveFailed).isTrue();
  }

  @Test
  public void otherDataIntegrityViolationExceptionNotIgnored() {
    SmtpVisitEntity crawlResult = smtpVisit(randomUUID());
    crawlResult.setDomainName(StringUtils.repeat("a", 130));
    Assertions.assertThrows(
      DataIntegrityViolationException.class,
      () -> repository.saveAndIgnoreDuplicateKeys(crawlResult)
    );
  }

  @Test
  public void saveSuccessfulWhenWeCleanBinaryData() {
    SmtpVisitEntity smtpVisitEntity = smtpVisitWithBinaryData();
    // clean the data before saving
    for (SmtpHostEntity host : smtpVisitEntity.getHosts()) {
        host.getConversation().clean();
    }
    String actualCountry = smtpVisitEntity.getHosts().get(0).getConversation().getCountry();
    assertThat(actualCountry).isEqualTo("Jamaica ");
    logger.info("Before save: smtpVisitEntity.getVisitId() = {}", smtpVisitEntity.getVisitId());
    smtpVisitEntity = repository.save(smtpVisitEntity);
    logger.info("After save: crawlResult.getVisitId() = {}", smtpVisitEntity.getVisitId());
    assertThat(smtpVisitEntity.getVisitId()).isNotNull();
  }

  private SmtpVisitEntity smtpVisit(UUID uuid) {
    SmtpVisitEntity crawlResult = new SmtpVisitEntity(uuid, "jamaica.be");
    SmtpHostEntity host = new SmtpHostEntity("smtp1.example.com");
    SmtpConversation conversation = new SmtpConversation("1.2.3.4");
    conversation.setConnectReplyCode(220);
    conversation.setIpVersion(4);
    conversation.setBanner("my binary banner");
    conversation.setConnectionTimeMs(123);
    conversation.setStartTlsOk(false);
    conversation.setCountry("Jamaica");
    conversation.setAsnOrganisation("Happy Green grass");
    conversation.setAsn(654L);
    host.setConversation(conversation);
    crawlResult.add(host);
    return crawlResult;
  }

  private SmtpVisitEntity smtpVisitWithBinaryData() {
    UUID uuid = randomUUID();
    SmtpVisitEntity visitEntity = new SmtpVisitEntity(uuid, "dnsbelgium.be");
    SmtpHostEntity host = new SmtpHostEntity("smtp1.example.com");
    SmtpConversation conversation = new SmtpConversation("1.2.3.4");
    conversation.setConnectReplyCode(220);
    conversation.setIpVersion(4);
    conversation.setBanner("my binary \u0000 banner");
    conversation.setConnectionTimeMs(123);
    conversation.setStartTlsOk(false);
    conversation.setCountry("Jamaica \u0000");
    conversation.setAsnOrganisation("Happy \u0000 Green grass");
    conversation.setAsn(654L);
    host.setConversation(conversation);
    visitEntity.add(host);
    return visitEntity;
  }

}
