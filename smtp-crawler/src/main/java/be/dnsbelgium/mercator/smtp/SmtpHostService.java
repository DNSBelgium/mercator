package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class SmtpHostService {

  private final SmtpHostRepository hostRepository;

  public SmtpHostService(SmtpHostRepository hostRepository) {
    this.hostRepository = hostRepository;
  }

  public Optional<SmtpHostEntity> ipRecentlyCrawled(InetAddress ip) {
    return hostRepository.findRecentCrawlByIp(ip.getHostAddress(), ZonedDateTime.now());
  }
}
