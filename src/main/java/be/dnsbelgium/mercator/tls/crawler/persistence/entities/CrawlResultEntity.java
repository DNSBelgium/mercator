package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@Getter
@ToString
public class CrawlResultEntity {

  private String visitId;

  private String hostName;

  private String domainName;

  private Instant crawlTimestamp;

  private FullScanEntity fullScanEntity;

  private CertificateEntity leafCertificateEntity;

  private boolean certificateExpired;

  private boolean certificateTooSoon;

  private boolean chainTrustedByJavaPlatform;

  private boolean hostNameMatchesCertificate;

}
