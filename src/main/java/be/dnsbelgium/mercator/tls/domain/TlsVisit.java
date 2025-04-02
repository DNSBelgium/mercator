package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ToString
@Getter
@Setter
@NoArgsConstructor(force = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TlsVisit {

  private final String visitId;
  private final String domainName;

  private final FullScanEntity fullScanEntity;
  private final String hostName;
  private final boolean hostNameMatchesCertificate;
  private final boolean chainTrustedByJavaPlatform;
  private boolean certificateExpired;
  private boolean certificateTooSoon;
  private final Instant crawlTimestamp;
  private final List<String> certificateChainFingerprints;
  private final List<Certificate> certificateChain;

  private TlsVisit(
          String visitId,
          String domainName,
          String hostName,
          FullScanEntity fullScanEntity,
          FullScan fullScan,
          SingleVersionScan singleVersionScan
  ) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.crawlTimestamp = Instant.now();
    this.fullScanEntity = fullScanEntity;
    this.hostName = hostName;
    Certificate certificate = null;

    if (fullScan != null) {
      this.hostNameMatchesCertificate = fullScan.isHostNameMatchesCertificate();
      this.chainTrustedByJavaPlatform = fullScan.isChainTrustedByJavaPlatform();
      this.certificateChain = fullScan.getCertificateChain().get();
      if (fullScan.getPeerCertificate().isPresent()) {
        certificate = fullScan.getPeerCertificate().get();
      }
    } else {
      this.hostNameMatchesCertificate = (singleVersionScan != null) && singleVersionScan.isHostNameMatchesCertificate();
      this.chainTrustedByJavaPlatform = (singleVersionScan != null) && singleVersionScan.isChainTrustedByJavaPlatform();
      this.certificateChain = Optional.ofNullable(singleVersionScan.getCertificateChain()).orElse(List.of());

      if (singleVersionScan != null && singleVersionScan.getPeerCertificate() != null) {
        certificate = singleVersionScan.getPeerCertificate();
      }
    }
    if (certificate != null) {
      this.certificateTooSoon = Instant.now().isBefore(certificate.getNotBefore());
      this.certificateExpired = Instant.now().isAfter(certificate.getNotAfter());
    } else {
      this.certificateTooSoon = false;
      this.certificateExpired = false;
    }
    if (this.certificateChain != null) {
      this.certificateChainFingerprints = this.certificateChain.stream().map(Certificate::getSha256Fingerprint).toList();
    } else {
      this.certificateChainFingerprints = List.of();
    }
  }

  public static TlsVisit fromCache(
          String visitId,
          String domainName,
          String hostName,
          FullScanEntity fullScanEntity,
          SingleVersionScan singleVersionScan) {
    return new TlsVisit(visitId, domainName, hostName, fullScanEntity, null, singleVersionScan);
  }

  public static TlsVisit fromScan(String visitId, String domainName, String hostName, FullScan fullScan) {
    FullScanEntity fullScanEntity = convert(fullScan);
    return new TlsVisit(visitId, domainName, hostName, fullScanEntity, fullScan, null);
  }

  public static FullScanEntity convert(FullScan fullScan) {
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
        .crawlTimestamp(fullScan.getCrawlTimestamp())
        .build();
  }
}
