package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@Table(name = "ssl_crawl_result")
public class SslCrawlResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "visit_id")
  private UUID visitId;
  @Column(name = "domain_name")
  private String domainName;
  @Column(name = "crawl_timestamp", columnDefinition = "TIMESTAMPTZ")
  private Instant crawlTimestamp;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "ok")
  private Boolean ok;
  @Column(name = "problem")
  private String problem;

  @Column(name = "hostname_used_for_server_name_indication")
  private String hostnameUsedForServerNameIndication;
  @Column(name = "nb_certificate_deployed")
  private Integer nbCertificateDeployed;

  @Column(name = "support_ssl_2_0")
  private Boolean supportSsl_2_0;
  @Column(name = "support_ssl_3_0")
  private Boolean supportSsl_3_0;
  @Column(name = "support_tls_1_0")
  private Boolean supportTls_1_0;
  @Column(name = "support_tls_1_1")
  private Boolean supportTls_1_1;
  @Column(name = "support_tls_1_2")
  private Boolean supportTls_1_2;
  @Column(name = "support_tls_1_3")
  private Boolean supportTls_1_3;
  @Column(name = "support_ecdh_key_exchange")
  private Boolean supportEcdhKeyExchange;

  @ElementCollection(fetch = FetchType.EAGER)
  @JoinColumns({ @JoinColumn(name = "ssl_crawl_result_id", referencedColumnName = "id")})
  private Set<CertificateDeployment> certificateDeployments;
}
