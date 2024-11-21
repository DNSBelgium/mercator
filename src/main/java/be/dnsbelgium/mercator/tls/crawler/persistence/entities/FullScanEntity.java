package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@Getter
@ToString
public class FullScanEntity {

  @Setter
  private String id;

  private Instant crawlTimestamp;

  private String ip;

  private String serverName;

  private boolean connectOk;

  private boolean supportTls_1_3;

  private boolean supportTls_1_2;

  private boolean supportTls_1_1;

  private boolean supportTls_1_0;

  private boolean supportSsl_3_0;

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

  private long millis_tls_1_3;
  private long millis_tls_1_2;
  private long millis_tls_1_1;
  private long millis_tls_1_0;
  private long millis_ssl_3_0;
  private long millis_ssl_2_0;

  public long getTotalDurationInMs() {
    return millis_tls_1_3 + millis_tls_1_2 + millis_tls_1_1 + millis_tls_1_0 + millis_ssl_3_0 + millis_ssl_2_0;
  }

  //public log get

  // This method is used to see if two domains on same IP have the same TLS configuration
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
