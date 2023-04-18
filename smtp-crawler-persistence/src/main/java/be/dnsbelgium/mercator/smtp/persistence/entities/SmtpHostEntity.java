package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "smtp_host")
public class SmtpHostEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  private String ip;

  private Integer asn;
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
  @ToString.Exclude
  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
    name = "host_extension",
    joinColumns = @JoinColumn(name = "extension_id"),
    inverseJoinColumns = @JoinColumn(name = "host_id"))
  private Set<ExtensionEntity> supportedExtensions = new HashSet<>();

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

  @ManyToMany(mappedBy = "hosts", cascade = CascadeType.PERSIST)
  private List<SmtpServerEntity> servers = new ArrayList<>();

  private ZonedDateTime timestamp = ZonedDateTime.now();

  public void addServer(SmtpServerEntity server) {
    servers.add(server);
  }

  public SmtpHostEntity fromSmtpHostIp(SmtpHostIp host) {
    this.setId(host.getId());
    this.setIp(host.getIp());
    this.setAsn(host.getAsn());
    this.setCountry(host.getCountry());
    this.setAsnOrganisation(host.getAsnOrganisation());
    this.setBanner(host.getBanner());
    this.setConnectOK(host.isConnectOK());
    this.setConnectReplyCode(host.getConnectReplyCode());
    Set<ExtensionEntity> extensionEntities = new HashSet<>();
    for (String extension : host.getSupportedExtensions()) {
      extensionEntities.add(new ExtensionEntity().fromString(extension));
    }
    this.setSupportedExtensions(extensionEntities);
    this.setIpVersion(host.getIpVersion());
    this.setStartTlsOk(host.isStartTlsOk());
    this.setErrorMessage(host.getErrorMessage());
    this.setConnectionTimeMs(host.getConnectionTimeMs());
    this.setSoftware(host.getSoftware());
    this.setSoftwareVersion(host.getSoftwareVersion());
    this.setTimestamp(host.getTimestamp());
    return this;
  }

  public SmtpHostIp toSmtpHostIp() {
    SmtpHostIp hostIp = new SmtpHostIp();
    hostIp.setId(id);
    hostIp.setIp(ip);
    hostIp.setAsn(asn);
    hostIp.setCountry(country);
    hostIp.setAsnOrganisation(asnOrganisation);
    hostIp.setBanner(banner);
    hostIp.setConnectOK(connectOK);
    hostIp.setConnectReplyCode(connectReplyCode);
    Set<String> extensions = new HashSet<>();
    for (ExtensionEntity extension : supportedExtensions) {
      extensions.add(extension.getName());
    }
    hostIp.setSupportedExtensions(extensions);
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
