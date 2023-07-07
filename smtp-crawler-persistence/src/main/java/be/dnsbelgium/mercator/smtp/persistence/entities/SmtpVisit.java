package be.dnsbelgium.mercator.smtp.persistence.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "smtp_visit")
public class SmtpVisit {
  @Id
  @Column(name = "visit_id")
  private UUID visitId;

  @Column(name = "domain_name")
  private String domainName;

  private ZonedDateTime timestamp = ZonedDateTime.now();

  @Column(name = "num_conversations")
  private int numConversations = 0;

  @OneToMany(mappedBy = "visit", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @ToString.Exclude
  private List<SmtpHost> hosts = new ArrayList<>();

  private static final Logger logger = getLogger(SmtpVisit.class);

  @Enumerated(EnumType.STRING)
  @Column(name = "crawl_status")
  private CrawlStatus crawlStatus;

  public SmtpVisit(UUID visitId, String domainName) {
    logger.debug("Creating new SmtpVisit with visitId={} and domainName={}", visitId, domainName);
    this.visitId = visitId;
    this.domainName = domainName;
  }

  public void add(SmtpHost host) {
    host.setVisit(this);
    hosts.add(host);
    ++numConversations;
  }

  public void add(List<SmtpHost> smtpHostEntities) {
    for (SmtpHost host : smtpHostEntities){
      add(host);
    }
  }
}
