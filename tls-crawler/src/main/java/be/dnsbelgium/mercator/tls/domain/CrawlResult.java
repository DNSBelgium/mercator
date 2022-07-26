package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.Certificate;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.ScanResult;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.TlsScanResult;
import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.Instant.now;

public class CrawlResult {

  private final ZonedDateTime crawlTimestamp;

  private final ProtocolScanResult protocolScanResult;

  private final VisitRequest visitRequest;

  private final TlsCrawlResult tlsCrawlResult;

  private final ScanResult scanResult;

  private CrawlResult(VisitRequest visitRequest, ProtocolScanResult protocolScanResult, ScanResult scanResult, TlsCrawlResult tlsCrawlResult) {
    this.crawlTimestamp = ZonedDateTime.now();
    this.protocolScanResult = protocolScanResult;
    this.visitRequest = visitRequest;
    this.tlsCrawlResult = tlsCrawlResult;
    this.scanResult = scanResult;
  }

  public static CrawlResult fromCache(VisitRequest visitRequest, ScanResult scanResult, ProtocolScanResult protocolScanResult) {
    return new CrawlResult(visitRequest, protocolScanResult, scanResult, null);
  }

  public static CrawlResult fromScan(VisitRequest visitRequest, TlsCrawlResult tlsCrawlResult) {
    ZonedDateTime crawlTimestamp = ZonedDateTime.now();
    ScanResult scanResult = convert(crawlTimestamp, tlsCrawlResult);
    return new CrawlResult(visitRequest, null, scanResult, tlsCrawlResult);
  }

  public ScanResult getScanResult() {
    return scanResult;
  }

  public boolean isFresh() {
    return tlsCrawlResult != null;
  }

  public boolean hostNameMatchesCertificate() {
    if (tlsCrawlResult != null) {
      return tlsCrawlResult.isHostNameMatchesCertificate();
    }
    if (protocolScanResult != null) {
      return protocolScanResult.isHostNameMatchesCertificate();
    }
    return false;
  }

  public boolean chainTrustedByJavaPlatform() {
    if (tlsCrawlResult != null) {
      return tlsCrawlResult.isChainTrustedByJavaPlatform();
    }
    if (protocolScanResult != null) {
      return protocolScanResult.isChainTrustedByJavaPlatform();
    }
    return false;
  }

  public Optional<List<CertificateInfo>> getCertificateChain() {
    if (tlsCrawlResult != null) {
      return tlsCrawlResult.getCertificateChain();
    }
    if (protocolScanResult != null) {
      return Optional.of(protocolScanResult.getCertificateChain());
    }
    return Optional.empty();
  }

  public Optional<CertificateInfo> getPeerCertificate() {
    if (tlsCrawlResult != null) {
      return tlsCrawlResult.getPeerCertificate();
    }
    if (protocolScanResult != null) {
      return Optional.of(protocolScanResult.getPeerCertificate());
    }
    return Optional.empty();
  }

  public TlsScanResult convertToEntity() {
    Optional<CertificateInfo> peerCertificate = this.getPeerCertificate();
    boolean certificateExpired = peerCertificate.isPresent() && now().isAfter(peerCertificate.get().getNotAfter());
    boolean certificateTooSoon = peerCertificate.isPresent() && now().isBefore(peerCertificate.get().getNotBefore());
    Certificate leafCertificate = peerCertificate.map(CertificateInfo::asEntity).orElse(null);

    return TlsScanResult.builder()
        .scanResult(this.scanResult)
        .visitId(this.visitRequest.getVisitId())
        .domainName(this.visitRequest.getDomainName())
        .hostName(this.visitRequest.getDomainName())
        .crawlTimestamp(this.crawlTimestamp)
        .leafCertificate(leafCertificate)
        .certificateExpired(certificateExpired)
        .certificateTooSoon(certificateTooSoon)
        .hostNameMatchesCertificate(this.hostNameMatchesCertificate())
        .chainTrustedByJavaPlatform(this.chainTrustedByJavaPlatform())
        .build();
  }

  public static ScanResult convert(ZonedDateTime timestamp,  TlsCrawlResult tlsCrawlResult) {
    var tls13 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_3);
    var tls12 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_2);
    var tls11 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_1);
    var tls10 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_0);
    var ssl3 = tlsCrawlResult.get(TlsProtocolVersion.SSL_3);
    var ssl2 = tlsCrawlResult.get(TlsProtocolVersion.SSL_2);

    String lowestVersion  = tlsCrawlResult.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    String highestVersion = tlsCrawlResult.getHighestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);

    return ScanResult.builder()
        .lowestVersionSupported(lowestVersion)
        .highestVersionSupported(highestVersion)
        .supportTls_1_3(tls13.isHandshakeOK())
        .supportTls_1_2(tls12.isHandshakeOK())
        .supportTls_1_1(tls11.isHandshakeOK())
        .supportTls_1_0(tls10.isHandshakeOK())
        .supportSsl_3_0(ssl3.isHandshakeOK())
        .supportSsl_2_0(ssl2.isHandshakeOK())
        .errorTls_1_3(tls13.getErrorMessage())
        .errorTls_1_2(tls12.getErrorMessage())
        .errorTls_1_1(tls11.getErrorMessage())
        .errorTls_1_0(tls10.getErrorMessage())
        .errorSsl_3_0(ssl3.getErrorMessage())
        .errorSsl_2_0(ssl2.getErrorMessage())
        .millis_tls_1_0(tls10.getScanDuration().toMillis())
        .millis_tls_1_1(tls11.getScanDuration().toMillis())
        .millis_tls_1_2(tls12.getScanDuration().toMillis())
        .millis_tls_1_3(tls13.getScanDuration().toMillis())
        .millis_ssl_3_0(ssl3.getScanDuration().toMillis())
        .millis_ssl_2_0(ssl2.getScanDuration().toMillis())
        .ip(tls13.getIpAddress())
        .connectOk(tls13.isConnectOK())
        .serverName(tls13.getServerName())
        .selectedCipherTls_1_3(tls13.getSelectedCipherSuite())
        .selectedCipherTls_1_2(tls12.getSelectedCipherSuite())
        .selectedCipherTls_1_1(tls11.getSelectedCipherSuite())
        .selectedCipherTls_1_0(tls10.getSelectedCipherSuite())
        .selectedCipherSsl_3_0(ssl3.getSelectedCipherSuite())
        .crawlTimestamp(timestamp)
        .build();
  }
}
