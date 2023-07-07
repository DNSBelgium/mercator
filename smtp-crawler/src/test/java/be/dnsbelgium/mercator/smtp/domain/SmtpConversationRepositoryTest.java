package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
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
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void saveConversationTest(){
    SmtpConversation conversation = new SmtpConversation();
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
    Optional<SmtpConversation> conversationFromDb = repository.findById(repository.save(conversation).getId());
    assertThat(conversationFromDb.isPresent()).isTrue();
    @SuppressWarnings("OptionalGetWithoutIsPresent") SmtpConversation smtpConversation = conversationFromDb.get();
    assertThat(smtpConversation.getIp()).isEqualTo("1.2.3.4");
    assertThat(smtpConversation.getIpVersion()).isEqualTo(4);
    assertThat(smtpConversation.getTimestamp()).isEqualTo(timestamp);
    assertThat(smtpConversation.getBanner()).isEqualTo("Welcome");
    assertThat(smtpConversation.getAsn()).isEqualTo(2147483648L);
    assertThat(smtpConversation.getAsnOrganisation()).isEqualTo("AsnOrganisation");
    assertThat(smtpConversation.isStartTlsOk()).isTrue();
    assertThat(smtpConversation.getStartTlsReplyCode()).isEqualTo(10);
    assertThat(smtpConversation.getErrorMessage()).isEqualTo("[1.2.3.4] Timed out waiting for a response to [initial response]");
    assertThat(smtpConversation.isConnectOK()).isTrue();
    assertThat(smtpConversation.getConnectionTimeMs()).isEqualTo(567);
    assertThat(smtpConversation.getSoftware()).isEqualTo("MailSoftware");
    assertThat(smtpConversation.getSoftwareVersion()).isEqualTo("1.3");
    assertThat(smtpConversation.getCountry()).isEqualTo("Belgium");
    assertThat(smtpConversation.getSupportedExtensions()).isEqualTo(extensions);
  }

  @Test
  void findAllByVisitId(){
    SmtpConversation conversation = new SmtpConversation();
    conversation.setIp("4.5.6.7");
    conversation.setIpVersion(4);
    conversation.setCountry("Belgium");

    UUID visitId = UUID.randomUUID();
    SmtpVisit visit = new SmtpVisit();
    SmtpHost host = new SmtpHost();
    host.setHostName("dns.be");
    host.setFromMx(true);
    host.setPriority(10);
    host.setConversation(conversation);
    visit.setVisitId(visitId);
    visit.setDomainName("dnsbelgium.be");
    visit.add(host);
    hostRepository.save(host);
    List<SmtpConversation> conversations = repository.findAllByVisitId(visitId);
    Assertions.assertThat(conversations.size()).isEqualTo(1);
  }
}
