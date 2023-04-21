package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "local"})
public class SmtpConversationServiceTest {
  @Autowired
  private SmtpConversationService service;
  @Autowired
  private SmtpConversationRepository conversationRepository;

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void ipRecentlyCrawledTest() throws UnknownHostException {
    SmtpConversationEntity conversation = new SmtpConversationEntity();
    conversation.setIp("1.2.3.4");
    conversation.setIpVersion(4);
    conversationRepository.save(conversation);
    InetAddress ip = InetAddress.getByName("1.2.3.4");
    Optional<SmtpConversationEntity> conversationEntity = service.ipRecentlyCrawled(ip);
    assertThat(conversationEntity).isPresent();
    assertThat(conversationEntity.get().getIp()).isEqualTo("1.2.3.4");
  }
}
