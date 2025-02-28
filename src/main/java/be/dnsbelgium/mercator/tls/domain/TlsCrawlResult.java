package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ToString
@NoArgsConstructor(force = true)
public class TlsCrawlResult {

  private final Instant crawlTimestamp;

  private final String hostName;

  private final SingleVersionScan singleVersionScan;

  @Getter
  private final VisitRequest visitRequest;

  private final FullScan fullScan;

  @Getter
  private final FullScanEntity fullScanEntity;

  public void setCertificateChain(List<be.dnsbelgium.mercator.tls.domain.certificates.Certificate> chain) {
    System.out.println("setCertificateChain: " + chain);
  }

  public void setPeerCertificate(Certificate certificate) {
    System.out.println("setPeerCertificate: " + certificate);
  }

  public void setFresh(Boolean fresh) {
    System.out.println("setFresh: " + fresh);
  }
  private TlsCrawlResult(String hostName, VisitRequest visitRequest, SingleVersionScan singleVersionScan, FullScanEntity fullScanEntity, FullScan fullScan) {
    this.crawlTimestamp = Instant.now();
    this.singleVersionScan = singleVersionScan;
    this.visitRequest = visitRequest;
    this.fullScan = fullScan;
    this.fullScanEntity = fullScanEntity;
    this.hostName = hostName;
  }

  public static TlsCrawlResult fromCache(String hostName, VisitRequest visitRequest, FullScanEntity fullScanEntity, SingleVersionScan singleVersionScan) {
    return new TlsCrawlResult(hostName, visitRequest, singleVersionScan, fullScanEntity, null);
  }

  public static TlsCrawlResult fromScan(String hostName, VisitRequest visitRequest, FullScan fullScan) {
    Instant crawlTimestamp = Instant.now();
    FullScanEntity fullScanEntity = convert(crawlTimestamp, fullScan);
    return new TlsCrawlResult(hostName, visitRequest, null, fullScanEntity, fullScan);
  }

  public boolean isFresh() {
    return fullScan != null;
  }

  public boolean hostNameMatchesCertificate() {
    if (fullScan != null) {
      return fullScan.isHostNameMatchesCertificate();
    }
    if (singleVersionScan != null) {
      return singleVersionScan.isHostNameMatchesCertificate();
    }
    return false;
  }

  public boolean chainTrustedByJavaPlatform() {
    if (fullScan != null) {
      return fullScan.isChainTrustedByJavaPlatform();
    }
    if (singleVersionScan != null) {
      return singleVersionScan.isChainTrustedByJavaPlatform();
    }
    return false;
  }

  public Optional<List<be.dnsbelgium.mercator.tls.domain.certificates.Certificate>> getCertificateChain() {
    if (fullScan != null) {
      return fullScan.getCertificateChain();
    }
    if (singleVersionScan != null) {
      return Optional.ofNullable(singleVersionScan.getCertificateChain());
    }
    return Optional.empty();
  }

  public Optional<be.dnsbelgium.mercator.tls.domain.certificates.Certificate> getPeerCertificate() {
    if (fullScan != null) {
      return fullScan.getPeerCertificate();
    }
    if (singleVersionScan != null) {
      return Optional.ofNullable(singleVersionScan.getPeerCertificate());
    }
    return Optional.empty();
  }

  public static FullScanEntity convert(Instant timestamp, FullScan fullScan) {
    var tls13 = fullScan.get(TlsProtocolVersion.TLS_1_3);
    var tls12 = fullScan.get(TlsProtocolVersion.TLS_1_2);
    var tls11 = fullScan.get(TlsProtocolVersion.TLS_1_1);
    var tls10 = fullScan.get(TlsProtocolVersion.TLS_1_0);
    var ssl3 = fullScan.get(TlsProtocolVersion.SSL_3);
    var ssl2 = fullScan.get(TlsProtocolVersion.SSL_2);

    String lowestVersion  = fullScan.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    String highestVersion = fullScan.getHighestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);

    return FullScanEntity.builder()
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
