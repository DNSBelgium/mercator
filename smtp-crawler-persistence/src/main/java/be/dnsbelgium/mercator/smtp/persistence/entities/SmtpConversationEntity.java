package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "smtp_conversation")
public class SmtpConversationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  private String ip;

  private Long asn;
  private String country;
  @Column(name = "asn_organisation")
  private String asnOrganisation;

  // Welcome message received from server
  private String banner;

  // True if connection was successful
  @Column(name = "connect_ok")
  private boolean connectOK = false;

  // Reply code received after connect
  @Column(name = "connect_reply_code")
  private int connectReplyCode;

  // extensions reported in response to EHLO
  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb", name = "extensions")
  public Set<String> supportedExtensions = new HashSet<>();

  // IP version (4 or 6)
  @Column(name = "ip_version")
  private int ipVersion;

  // True if STARTTLS is supported  (TODO:  succesful or reported to be supported ?)
  @Column(name = "start_tls_ok")
  private boolean startTlsOk = false;

  // Reply code received after STARTTLS command
  @Column(name = "start_tls_reply_code")
  private int startTlsReplyCode;

  // Error message from crawler, in case something went wrong
  @Column(name = "error_message")
  private String errorMessage;

  // Time (ms) it took to create a connection
  @Column(name = "connection_time_ms")
  private long connectionTimeMs = -1;

  // SMTP software detected from banner (wild guess, easy to spoof)
  private String software;

  // SMTP software version detected from banner (wild guess, easy to spoof)
  @Column(name = "software_version")
  private String softwareVersion;

  @OneToMany(mappedBy = "conversation", cascade = CascadeType.PERSIST)
  private List<SmtpHostEntity> hosts = new ArrayList<>();

  private ZonedDateTime timestamp = ZonedDateTime.now();

  public void addServer(SmtpHostEntity host) {
    hosts.add(host);
  }

  public SmtpConversationEntity fromSmtpHostIp(SmtpConversation host) {
    this.setId(host.getId());
    this.setIp(host.getIp());
    this.setAsn(host.getAsn());
    this.setCountry(host.getCountry());
    this.setAsnOrganisation(host.getAsnOrganisation());
    this.setBanner(host.getBanner());
    this.setConnectOK(host.isConnectOK());
    this.setConnectReplyCode(host.getConnectReplyCode());
    this.setSupportedExtensions(host.getSupportedExtensions());
    this.setIpVersion(host.getIpVersion());
    this.setStartTlsOk(host.isStartTlsOk());
    this.setErrorMessage(host.getErrorMessage());
    this.setConnectionTimeMs(host.getConnectionTimeMs());
    this.setSoftware(host.getSoftware());
    this.setSoftwareVersion(host.getSoftwareVersion());
    this.setTimestamp(host.getTimestamp());
    return this;
  }

  public SmtpConversation toSmtpHostIp() {
    SmtpConversation hostIp = new SmtpConversation();
    hostIp.setId(id);
    hostIp.setIp(ip);
    hostIp.setAsn(asn);
    hostIp.setCountry(country);
    hostIp.setAsnOrganisation(asnOrganisation);
    hostIp.setBanner(banner);
    hostIp.setConnectOK(connectOK);
    hostIp.setConnectReplyCode(connectReplyCode);
    hostIp.setSupportedExtensions(supportedExtensions);
    hostIp.setIpVersion(ipVersion);
    hostIp.setStartTlsOk(startTlsOk);
    hostIp.setErrorMessage(errorMessage);
    hostIp.setConnectionTimeMs(connectionTimeMs);
    hostIp.setSoftware(software);
    hostIp.setSoftwareVersion(softwareVersion);
    hostIp.setTimestamp(timestamp);
    return hostIp;
  }
}
