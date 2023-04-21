package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class SmtpConversationService {

  @Value("${smtp.crawler.recently}")
  private Duration recently;

  private final SmtpConversationRepository conversationRepository;

  public SmtpConversationService(SmtpConversationRepository conversationRepository) {
    this.conversationRepository = conversationRepository;
  }

  public Optional<SmtpConversationEntity> ipRecentlyCrawled(InetAddress ip) {
    ZonedDateTime tempDate = ZonedDateTime.now().minus(recently);
    return conversationRepository.findRecentCrawlByIp(ip.getHostAddress(), tempDate);
  }
}
