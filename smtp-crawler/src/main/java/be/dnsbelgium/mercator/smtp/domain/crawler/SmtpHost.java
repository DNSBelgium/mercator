package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmtpHost {

  // TODO: check if we need this.
  private SmtpVisit visit;
  private Boolean fromMx;
  private String hostName;
  private int priority;
  private SmtpConversationEntity smtpConversationEntity;
  private SmtpConversation smtpConversation;

  public SmtpHost(){

  }

  public SmtpHost(SmtpVisit visit, Boolean fromMx, String hostName, int priority, SmtpConversationEntity smtpConversationEntity, SmtpConversation smtpConversation) {
    this.visit = visit;
    this.fromMx = fromMx;
    this.hostName = hostName;
    this.priority = priority;
    this.smtpConversationEntity = smtpConversationEntity;
    this.smtpConversation = smtpConversation;
  }

  public SmtpHost fromCache(SmtpVisit visit, Boolean fromMx, String hostName, int priority, SmtpConversationEntity smtpConversationEntity){
    return new SmtpHost(visit, fromMx, hostName, priority, smtpConversationEntity, null);
  }

  public SmtpHost fromCrawl(SmtpVisit visit, Boolean fromMx, String hostName, int priority, SmtpConversation smtpConversation){
    return new SmtpHost(visit, fromMx, hostName, priority, new SmtpConversationEntity().fromSmtpConversation(smtpConversation), smtpConversation);
  }

  public boolean isFresh(){
    return smtpConversation != null;
  }

  public SmtpHostEntity toEntity(){
    SmtpHostEntity smtpHost = new SmtpHostEntity();
    smtpHost.setHostName(hostName);
    smtpHost.setPriority(priority);
    smtpHost.setFromMx(fromMx);
    smtpHost.setConversation(smtpConversationEntity);
    return smtpHost;
  }
}
