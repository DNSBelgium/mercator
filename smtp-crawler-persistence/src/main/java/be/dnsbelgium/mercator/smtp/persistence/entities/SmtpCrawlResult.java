package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.SmtpServer;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
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
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "smtp_crawl_result")
public class SmtpCrawlResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;
  @Column(name = "visit_id")
  private UUID visitId;
  @Column(name = "domain_name")
  private String domainName;
  @Column(name = "crawl_status")
  private int crawlStatus;
  @Column(name = "crawl_timestamp")
  private ZonedDateTime crawlTimestamp;

  @Type(type = "jsonb")
  @Column(name = "servers", columnDefinition = "jsonb")
  private List<SmtpServer> servers = new ArrayList<>();

  @ToString.Exclude
  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
    name = "crawl_result_server",
    joinColumns = @JoinColumn(name = "crawl_id"),
    inverseJoinColumns = @JoinColumn(name = "server_id"))
  private List<SmtpServerEntity> smtpServerEntities = new ArrayList<>();

  private static final Logger logger = getLogger(SmtpCrawlResult.class);

  public SmtpCrawlResult(UUID visitId, String domainName) {
    logger.debug("Creating new SmtpCrawlResult with visitId={} and domainName={}", visitId, domainName);
    this.visitId = visitId;
    this.domainName = domainName;
    this.crawlTimestamp = ZonedDateTime.now();
  }

  public void add(SmtpServer server) {
    SmtpServerEntity entity = new SmtpServerEntity().fromSmtpServer(server);
    entity.addCrawlResult(this);
    this.smtpServerEntities.add(entity);
  }

  public List<SmtpServer> getServers() {
    List<SmtpServer> smtpServers = new ArrayList<>();
    for (SmtpServerEntity entity : smtpServerEntities) {
      smtpServers.add(entity.toSmtpServer());
    }
    return smtpServers;
  }
}
