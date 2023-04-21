package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpVisitRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SmtpVisitService {
  private final SmtpVisitRepository visitRepository;
  private final SmtpConversationRepository conversationRepository;
  private final SmtpHostRepository hostRepository;

  public SmtpVisitService(SmtpVisitRepository visitRepository, SmtpConversationRepository conversationRepository, SmtpHostRepository hostRepository) {
    this.visitRepository = visitRepository;
    this.conversationRepository = conversationRepository;
    this.hostRepository = hostRepository;
  }

  @Transactional
  public void save(SmtpVisitEntity smtpVisitEntity){
    List<SmtpHostEntity> hosts = smtpVisitEntity.getHosts();
    smtpVisitEntity.setHosts(null);
    SmtpVisitEntity savedVisit = visitRepository.save(smtpVisitEntity);

    for(SmtpHostEntity host : hosts){
      SmtpConversationEntity smtpConversationEntity = conversationRepository.save(host.getConversation());
      host.setConversation(smtpConversationEntity);
      host.setVisit(savedVisit);
      host.setVisitId(smtpVisitEntity.getVisitId());
      host.setConversationId(smtpConversationEntity.getId());
      hostRepository.save(host);
    }
  }
}
