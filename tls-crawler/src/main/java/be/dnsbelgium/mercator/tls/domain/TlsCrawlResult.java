package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;
import lombok.Getter;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Getter
public class TlsCrawlResult {

  private final Map<TlsProtocolVersion, ProtocolScanResult> scanResultMap = new HashMap<>();

  private final boolean connectOK;

  private static final Logger logger = getLogger(TlsCrawlResult.class);

  public TlsCrawlResult(boolean connectOK) {
    this.connectOK = connectOK;
  }

  public static TlsCrawlResult connectFailed(InetSocketAddress address, String errorMessage) {
    TlsCrawlResult crawlResult = new TlsCrawlResult(false);
    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      ProtocolScanResult result = ProtocolScanResult.of(version, address);
      result.setConnectOK(false);
      result.setErrorMessage(errorMessage);
      crawlResult.add(result);
    }
    return crawlResult;
  }

  public void add(ProtocolScanResult scanResult) {
    TlsProtocolVersion version = scanResult.getProtocolVersion();
    ProtocolScanResult existingResult = scanResultMap.get(version);
    if (existingResult != null) {
      throw new IllegalStateException("We already have a result for " + version);
    }
    scanResultMap.put(version, scanResult);
  }

  public ProtocolScanResult get(TlsProtocolVersion version) {
    return scanResultMap.get(version);
  }

  public void checkEachVersionFoundSameCertificate() {
    StringBuilder message = new StringBuilder();
    message.append("Certificates for ").append(getServerName()).append(":\n");
    Set<String> fingerPrints = new HashSet<>();
    for (TlsProtocolVersion version : scanResultMap.keySet()) {
      CertificateInfo cert = scanResultMap.get(version).getPeerCertificate();
      if (cert != null) {
        fingerPrints.add(cert.getSha256Fingerprint());
        message.append(version.getName()).append(" => ").append(cert.getSha256Fingerprint()).append("\n");
      } else {
        message.append(version.getName()).append(" => ").append("No certificate found").append("\n");
      }
    }
    if (fingerPrints.size() > 1) {
      logger.warn("Not every TLS/SSL version resulted in the same peer certifcate: {}", message);
    }
  }

  public Optional<List<CertificateInfo>> getCertificateChain() {
    // In theory, it's possible that scans for different TLS versions find different certificates.
    // But for now we just log a warning and use the first chain we find in the ProtocolScanResult's
    checkEachVersionFoundSameCertificate();
    return scanResultMap.values()
        .stream()
        .filter(ProtocolScanResult::hasCertificateChain)
        .map(ProtocolScanResult::getCertificateChain)
        .findFirst();
  }

  public Optional<CertificateInfo> getPeerCertificate() {
    return getCertificateChain()
        .stream()
        .filter(list -> !list.isEmpty())
        .map(x -> x.get(0))
        .findFirst();
  }

  public Optional<TlsProtocolVersion> getLowestVersionSupported() {
    return scanResultMap.values()
        .stream()
        .filter(ProtocolScanResult::isHandshakeOK)
        .map(ProtocolScanResult::getProtocolVersion)
        .min(Comparator.comparingInt(TlsProtocolVersion::valueAsInt));
  }

  public Optional<TlsProtocolVersion> getHighestVersionSupported() {
    return scanResultMap.values()
        .stream()
        .filter(ProtocolScanResult::isHandshakeOK)
        .map(ProtocolScanResult::getProtocolVersion)
        .max(Comparator.comparingInt(TlsProtocolVersion::valueAsInt));
  }

  public Optional<String> getServerName() {
    return scanResultMap.values()
        .stream()
        .map(ProtocolScanResult::getServerName)
        .filter(Objects::nonNull)
        .findFirst();
  }

  public boolean isChainTrustedByJavaPlatform() {
    return scanResultMap.values()
        .stream()
        .anyMatch(ProtocolScanResult::isChainTrustedByJavaPlatform);
  }

  public boolean isHostNameMatchesCertificate() {
    return scanResultMap.values()
        .stream()
        .anyMatch(ProtocolScanResult::isHostNameMatchesCertificate);
  }

}
