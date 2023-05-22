package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class SmtpHostRepositoryTest {
  @Autowired
  SmtpHostRepository repository;
  @Autowired
  SmtpConversationRepository conversationRepository;
  @Autowired
  ApplicationContext context;
  @Autowired
  JdbcTemplate jdbcTemplate;

  private static final Logger logger = getLogger(SmtpHostRepositoryTest.class);

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void saveHostTest(){
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
    SmtpHostEntity savedHost = repository.save(host);
    assertThat(savedHost.getHostName()).isEqualTo("dns.be");
    assertThat(savedHost.isFromMx()).isTrue();
    assertThat(savedHost.getPriority()).isEqualTo(10);
    assertThat(savedHost.getConversation()).isEqualTo(conversation);
  }

  @Test
  void findAllByVisitIdTest(){
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
    repository.save(host);
    List<SmtpHostEntity> hosts = repository.findAllByVisitId(visitId);
    assertThat(hosts.size()).isEqualTo(1);
  }

}
