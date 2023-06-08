package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Getter
@Setter
public class SmtpVisit {
  private UUID visitId;
  private String domainName;
  private ZonedDateTime timestamp = ZonedDateTime.now();
  private int numConversations = 0;
  private List<SmtpHost> hosts = new ArrayList<>();
  private static final Logger logger = getLogger(SmtpVisitEntity.class);
  private CrawlStatus crawlStatus;

  public void add(SmtpHost host) {
    //TODO kijken of de setVisit nodig is
    host.setVisit(this);
    hosts.add(host);
    numConversations++;
  }

  public void add(List<SmtpHost> smtpHosts) {
    for (SmtpHost host : smtpHosts){
      add(host);
    }
  }

  public SmtpVisitEntity toEntity(){
    SmtpVisitEntity smtpVisitEntity = new SmtpVisitEntity();
    smtpVisitEntity.setVisitId(visitId);
    smtpVisitEntity.setTimestamp(timestamp);
    smtpVisitEntity.setDomainName(domainName);
    smtpVisitEntity.setCrawlStatus(crawlStatus);
    smtpVisitEntity.setNumConversations(numConversations);
    List<SmtpHostEntity> smtpHostEntities = new ArrayList<>();
    for (SmtpHost host : hosts){
      SmtpHostEntity hostEntity = host.toEntity();
      hostEntity.setVisit(smtpVisitEntity);
      smtpHostEntities.add(hostEntity);
    }
    smtpVisitEntity.setHosts(smtpHostEntities);
    return smtpVisitEntity;
  }
}
