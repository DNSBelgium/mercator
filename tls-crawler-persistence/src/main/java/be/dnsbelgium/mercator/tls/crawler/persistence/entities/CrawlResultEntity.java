package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@Getter
@ToString
@Table(name = "crawl_result")
public class CrawlResultEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "visit_id")
  private UUID visitId;

  @Column(name = "host_name")
  private String hostName;

  @Column(name = "domain_name")
  private String domainName;

  @Column(name = "crawl_timestamp")
  private ZonedDateTime crawlTimestamp;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "full_scan")
  private FullScanEntity fullScanEntity;

  @JoinColumn(name = "leaf_certificate")
  @ManyToOne
  private CertificateEntity leafCertificateEntity;

  @Column(name = "certificate_expired")
  private boolean certificateExpired;

  @Column(name = "certificate_too_soon")
  private boolean certificateTooSoon;

  @Column(name = "chain_trusted_by_java_platform")
  private boolean chainTrustedByJavaPlatform;

  @Column(name = "host_name_matches_certificate")
  private boolean hostNameMatchesCertificate;

}
