package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class SmtpConversationRepositoryTest {
  @Autowired
  SmtpConversationRepository repository;
  @Autowired
  SmtpHostRepository hostRepository;
  @Autowired
  ApplicationContext context;
  @Autowired
  JdbcTemplate jdbcTemplate;

  private static final Logger logger = getLogger(SmtpConversationRepositoryTest.class);

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void saveConversationTest(){
    SmtpConversationEntity conversation = new SmtpConversationEntity();
    ZonedDateTime timestamp = ZonedDateTime.now();
    conversation.setIp("1.2.3.4");
    conversation.setIpVersion(4);
    conversation.setTimestamp(timestamp);
    conversation.setBanner("Welcome");
    conversation.setAsn(2147483648L);
    conversation.setAsnOrganisation("AsnOrganisation");
    conversation.setStartTlsOk(true);
    conversation.setStartTlsReplyCode(10);
    conversation.setErrorMessage("[1.2.3.4] Timed out waiting for a response to [initial response]");
    conversation.setConnectOK(true);
    conversation.setConnectionTimeMs(567);
    conversation.setSoftware("MailSoftware");
    conversation.setSoftwareVersion("1.3");
    conversation.setCountry("Belgium");
    Set<String> extensions = new HashSet<>();
    extensions.add("Test");
    extensions.add("Ook een test");
    conversation.setSupportedExtensions(extensions);
    Optional<SmtpConversationEntity> conversationFromDb = repository.findById(repository.save(conversation).getId());
    assertThat(conversationFromDb.isPresent()).isTrue();
    SmtpConversationEntity conversationEntity = conversationFromDb.get();
    assertThat(conversationEntity.getIp()).isEqualTo("1.2.3.4");
    assertThat(conversationEntity.getIpVersion()).isEqualTo(4);
    assertThat(conversationEntity.getTimestamp()).isEqualTo(timestamp);
    assertThat(conversationEntity.getBanner()).isEqualTo("Welcome");
    assertThat(conversationEntity.getAsn()).isEqualTo(2147483648L);
    assertThat(conversationEntity.getAsnOrganisation()).isEqualTo("AsnOrganisation");
    assertThat(conversationEntity.isStartTlsOk()).isTrue();
    assertThat(conversationEntity.getStartTlsReplyCode()).isEqualTo(10);
    assertThat(conversationEntity.getErrorMessage()).isEqualTo("[1.2.3.4] Timed out waiting for a response to [initial response]");
    assertThat(conversationEntity.isConnectOK()).isTrue();
    assertThat(conversationEntity.getConnectionTimeMs()).isEqualTo(567);
    assertThat(conversationEntity.getSoftware()).isEqualTo("MailSoftware");
    assertThat(conversationEntity.getSoftwareVersion()).isEqualTo("1.3");
    assertThat(conversationEntity.getCountry()).isEqualTo("Belgium");
    assertThat(conversationEntity.getSupportedExtensions()).isEqualTo(extensions);
  }

  @Test
  void findAllByVisitId(){
    SmtpConversationEntity conversation = new SmtpConversationEntity();
    conversation.setIp("4.5.6.7");
    conversation.setIpVersion(4);
    conversation.setCountry("Belgium");

    UUID visitId = UUID.randomUUID();
    SmtpVisitEntity vistEntity = new SmtpVisitEntity();
    SmtpHostEntity host = new SmtpHostEntity();
    host.setHostName("dns.be");
    host.setFromMx(true);
    host.setPriority(10);
    host.setConversation(conversation);
    vistEntity.setVisitId(visitId);
    vistEntity.setDomainName("dnsbelgium.be");
    vistEntity.add(host);
    hostRepository.save(host);
    List<SmtpConversationEntity> conversations = repository.findAllByVisitId(visitId);
    Assertions.assertThat(conversations.size()).isEqualTo(1);
  }
}
