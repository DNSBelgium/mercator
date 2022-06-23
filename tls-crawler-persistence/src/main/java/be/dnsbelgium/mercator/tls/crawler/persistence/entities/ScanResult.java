package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Getter
@ToString
@Table(name = "scan_result")
public class ScanResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "crawl_timestamp")
  private ZonedDateTime crawlTimestamp;

  private String ip;

  private String serverName;

  private boolean connectOk;

  @Column(name = "support_tls_1_3")
  private boolean supportTls_1_3;

  @Column(name = "support_tls_1_2")
  private boolean supportTls_1_2;

  @Column(name = "support_tls_1_1")
  private boolean supportTls_1_1;

  @Column(name = "support_tls_1_0")
  private boolean supportTls_1_0;

  @Column(name = "support_ssl_3_0")
  private boolean supportSsl_3_0;

  @Column(name = "support_ssl_2_0")
  private boolean supportSsl_2_0;

  private String selectedCipherTls_1_3;
  private String selectedCipherTls_1_2;
  private String selectedCipherTls_1_1;
  private String selectedCipherTls_1_0;
  private String selectedCipherSsl_3_0;

  // TODO
  //private List<String> acceptedCipherSsl_2_0;

  private String lowestVersionSupported;
  private String highestVersionSupported;

  private String errorTls_1_3;
  private String errorTls_1_2;
  private String errorTls_1_1;
  private String errorTls_1_0;
  private String errorSsl_3_0;
  private String errorSsl_2_0;

  private boolean certificateExpired;
  private boolean certificateTooSoon;

  @JoinColumn(name = "leaf_certificate")
  @ManyToOne
  private Certificate leafCertificate;

  // This method is used to see if two domain's on same IP have the same TLS configuration
  public String summary() {
    return "connectOk=" + connectOk + ", " +
        "supportTls_1_3=" + supportTls_1_3 + ", " +
        "supportTls_1_2=" + supportTls_1_2 + ", " +
        "supportTls_1_1=" + supportTls_1_1 + ", " +
        "supportTls_1_0=" + supportTls_1_0 + ", " +
        "supportSsl_3_0=" + supportSsl_3_0 + ", " +
        "supportSsl_2_0=" + supportSsl_2_0 + ", " +
        "selectedCipherTls_1_3=" + selectedCipherTls_1_3 + ", " +
        "selectedCipherTls_1_2=" + selectedCipherTls_1_2 + ", " +
        "selectedCipherTls_1_1=" + selectedCipherTls_1_1 + ", " +
        "selectedCipherTls_1_0=" + selectedCipherTls_1_0 + ", " +
        "selectedCipherSsl_3_0=" + selectedCipherSsl_3_0;
  }

}
