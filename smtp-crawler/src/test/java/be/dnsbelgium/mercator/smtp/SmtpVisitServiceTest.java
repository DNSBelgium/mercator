package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpVisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "local"})
public class SmtpVisitServiceTest {
  @Autowired
  SmtpVisitService service;

  @Autowired
  SmtpVisitRepository repository;

  @Test
  void saveTest(){
    SmtpConversationEntity conversation1 = new SmtpConversationEntity();
    conversation1.setIp("1.2.3.4");
    conversation1.setIpVersion(4);

    SmtpConversationEntity conversation2 = new SmtpConversationEntity();
    conversation2.setIp("5.6.7.8");
    conversation2.setIpVersion(4);

    UUID visitId = UUID.randomUUID();
    SmtpVisitEntity visit = new SmtpVisitEntity();
    visit.setVisitId(visitId);
    visit.setDomainName("dnsbelgium.be");
    visit.setNumConversations(2);

    SmtpHostEntity host = new SmtpHostEntity();
    host.setConversation(conversation1);
    host.setHostName("protection.outlook.com");
    host.setPriority(0);
    host.setFromMx(true);
    host.setVisit(visit);

    List<SmtpHostEntity> hosts = new ArrayList<>();
    hosts.add(host);

    visit.setHosts(hosts);

    service.save(visit);

    Optional<SmtpVisitEntity> savedVisit = repository.findByVisitId(visitId);
    assertThat(savedVisit).isPresent();
    List<SmtpHostEntity> hostEntities = savedVisit.get().getHosts();

  }
}
