package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

  // True if STARTTLS is supported
  @Column(name = "start_tls_ok")
  private boolean startTlsOk = false;

  // Reply code received after STARTTLS command
  @Column(name = "start_tls_reply_code")
  private int startTlsReplyCode;

  // Error message from crawler, in case something went wrong
  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "error")
  @Enumerated(EnumType.STRING)
  private Error error;

  // Time (ms) it took to create a connection
  @Column(name = "connection_time_ms")
  private long connectionTimeMs = -1;

  // SMTP software detected from banner (wild guess, easy to spoof)
  private String software;

  // SMTP software version detected from banner (wild guess, easy to spoof)
  @Column(name = "software_version")
  private String softwareVersion;

  @OneToMany(mappedBy = "conversation", fetch = FetchType.EAGER)
  @ToString.Exclude
  private List<SmtpHostEntity> hosts = new ArrayList<>();

  private ZonedDateTime timestamp = ZonedDateTime.now();

  @Transient
  private final static String NULL_BYTE = "\u0000";
  @Transient
  private final static String EMPTY_STRING = "";

  public void setFromSmtpConversation(SmtpConversation conversation) {
    this.setId(conversation.getId());
    this.setIp(conversation.getIp());
    this.setAsn(conversation.getAsn());
    this.setCountry(conversation.getCountry());
    this.setAsnOrganisation(conversation.getAsnOrganisation());
    this.setBanner(conversation.getBanner());
    this.setConnectOK(conversation.isConnectOK());
    this.setConnectReplyCode(conversation.getConnectReplyCode());
    this.setSupportedExtensions(conversation.getSupportedExtensions());
    this.setIpVersion(conversation.getIpVersion());
    this.setStartTlsOk(conversation.isStartTlsOk());
    this.setErrorMessage(conversation.getErrorMessage());
    this.setError(conversation.getError());
    this.setConnectionTimeMs(conversation.getConnectionTimeMs());
    this.setSoftware(conversation.getSoftware());
    this.setSoftwareVersion(conversation.getSoftwareVersion());
    this.setTimestamp(conversation.getTimestamp());
  }

  public SmtpConversation toSmtpConversation() {
    SmtpConversation conversation = new SmtpConversation();
    conversation.setId(id);
    conversation.setIp(ip);
    conversation.setAsn(asn);
    conversation.setCountry(country);
    conversation.setAsnOrganisation(asnOrganisation);
    conversation.setBanner(banner);
    conversation.setConnectOK(connectOK);
    conversation.setConnectReplyCode(connectReplyCode);
    conversation.setSupportedExtensions(supportedExtensions);
    conversation.setIpVersion(ipVersion);
    conversation.setStartTlsOk(startTlsOk);
    conversation.setErrorMessage(errorMessage);
    conversation.setError(error);
    conversation.setConnectionTimeMs(connectionTimeMs);
    conversation.setSoftware(software);
    conversation.setSoftwareVersion(softwareVersion);
    conversation.setTimestamp(timestamp);
    return conversation;
  }

  public void clean() {
    this.country = clean(country);
    this.softwareVersion = clean(softwareVersion);
    this.software = clean(software);
    this.banner = clean(banner);
    this.asnOrganisation = clean(asnOrganisation);
    this.errorMessage = clean(errorMessage);
    this.ip = clean(ip);
    this.supportedExtensions = supportedExtensions.stream().map(SmtpConversationEntity::clean).collect(Collectors.toSet());
  }

  private static String clean(String input) {
    return StringUtils.replace(input, NULL_BYTE, EMPTY_STRING);
  }
}
