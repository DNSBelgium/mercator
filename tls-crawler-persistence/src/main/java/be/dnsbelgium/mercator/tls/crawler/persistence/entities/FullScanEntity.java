package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@Getter
@ToString
@Table(name = "full_scan")
public class FullScanEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "crawl_timestamp")
  private ZonedDateTime crawlTimestamp;

  private String ip;

  @Column(name = "server_name")
  private String serverName;

  @Column(name = "connect_ok")
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

  @Column(name = "selected_cipher_tls_1_3")
  private String selectedCipherTls_1_3;
  @Column(name = "selected_cipher_tls_1_2")
  private String selectedCipherTls_1_2;
  @Column(name = "selected_cipher_tls_1_1")
  private String selectedCipherTls_1_1;
  @Column(name = "selected_cipher_tls_1_0")
  private String selectedCipherTls_1_0;
  @Column(name = "selected_cipher_ssl_3_0")
  private String selectedCipherSsl_3_0;

  // TODO
  //private List<String> acceptedCipherSsl_2_0;

  @Column(name = "lowest_version_supported")
  private String lowestVersionSupported;
  @Column(name = "highest_version_supported")
  private String highestVersionSupported;

  @Column(name = "error_tls_1_3")
  private String errorTls_1_3;
  @Column(name = "error_tls_1_2")
  private String errorTls_1_2;
  @Column(name = "error_tls_1_1")
  private String errorTls_1_1;
  @Column(name = "error_tls_1_0")
  private String errorTls_1_0;
  @Column(name = "error_ssl_3_0")
  private String errorSsl_3_0;
  @Column(name = "error_ssl_2_0")
  private String errorSsl_2_0;

  private long millis_tls_1_3;
  private long millis_tls_1_2;
  private long millis_tls_1_1;
  private long millis_tls_1_0;
  private long millis_ssl_3_0;
  private long millis_ssl_2_0;

  @Column(name = "total_duration_in_ms")
  private long totalDurationInMs;

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
