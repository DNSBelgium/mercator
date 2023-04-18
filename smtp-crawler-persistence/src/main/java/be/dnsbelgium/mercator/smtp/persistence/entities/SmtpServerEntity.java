package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;
import be.dnsbelgium.mercator.smtp.dto.SmtpServer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "smtp_server")
public class SmtpServerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "host_name")
  private String hostName;

  private int priority;

  @ManyToMany(mappedBy = "smtpServerEntities", cascade = CascadeType.PERSIST)
  private List<SmtpCrawlResult> crawlResults = new ArrayList<>();

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
    name = "smtp_server_host",
    joinColumns = @JoinColumn(name = "server_id"),
    inverseJoinColumns = @JoinColumn(name = "host_id"))
  private List<SmtpHostEntity> hosts = new ArrayList<>();

  public void addCrawlResult(SmtpCrawlResult crawlResult) {
    crawlResults.add(crawlResult);
  }

  public SmtpServerEntity fromSmtpServer(SmtpServer smtpServer) {
    this.setHostName(smtpServer.getHostName());
    this.setPriority(smtpServer.getPriority());
    List<SmtpHostEntity> hostEntities = new ArrayList<>();
    for (SmtpHostIp host : smtpServer.getHosts()) {
      SmtpHostEntity entity = new SmtpHostEntity().fromSmtpHostIp(host);
      entity.addServer(this);
      hostEntities.add(entity);
    }
    this.setHosts(hostEntities);
    return this;
  }

  public SmtpServer toSmtpServer() {
    SmtpServer smtpServer = new SmtpServer(hostName, priority);
    for (SmtpHostEntity host : hosts) {
      smtpServer.addHost(host.toSmtpHostIp());
    }
    return smtpServer;
  }


}
