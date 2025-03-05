package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import lombok.Getter;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Getter
public class FullScan {

  private final Map<TlsProtocolVersion, SingleVersionScan> scanPerVersionMap = new HashMap<>();

  private final boolean connectOK;
  private final Instant crawlTimestamp;

  private static final Logger logger = getLogger(FullScan.class);

  public FullScan(boolean connectOK) {
    this.connectOK = connectOK;
    this.crawlTimestamp = Instant.now();
  }

  public static FullScan connectFailed(InetSocketAddress address, String errorMessage) {
    FullScan crawlResult = new FullScan(false);
    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      SingleVersionScan result = SingleVersionScan.of(version, address);
      result.setConnectOK(false);
      result.setErrorMessage(errorMessage);
      crawlResult.add(result);
    }
    return crawlResult;
  }

  public void add(SingleVersionScan singleVersionScan) {
    TlsProtocolVersion version = singleVersionScan.getProtocolVersion();
    SingleVersionScan existingResult = scanPerVersionMap.get(version);
    if (existingResult != null) {
      throw new IllegalStateException("We already have a result for " + version);
    }
    scanPerVersionMap.put(version, singleVersionScan);
  }

  public SingleVersionScan get(TlsProtocolVersion version) {
    return scanPerVersionMap.get(version);
  }

  public void checkEachVersionFoundSameCertificate() {
    StringBuilder message = new StringBuilder();
    message.append("Certificates for ").append(getServerName()).append(":\n");
    Set<String> fingerPrints = new HashSet<>();
    for (TlsProtocolVersion version : scanPerVersionMap.keySet()) {
      Certificate cert = scanPerVersionMap.get(version).getPeerCertificate();
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

  public Optional<List<Certificate>> getCertificateChain() {
    // In theory, it's possible that scans for different TLS versions find different certificates.
    // But for now we just log a warning and use the first chain we find in the SingleVersionScan's
    checkEachVersionFoundSameCertificate();
    return scanPerVersionMap.values()
        .stream()
        .filter(SingleVersionScan::hasCertificateChain)
        .map(SingleVersionScan::getCertificateChain)
        .findFirst();
  }

  public Optional<Certificate> getPeerCertificate() {
    return getCertificateChain()
        .stream()
        .filter(list -> !list.isEmpty())
        .map(List::getFirst)
        .findFirst();
  }

  public Optional<TlsProtocolVersion> getLowestVersionSupported() {
    return scanPerVersionMap.values()
        .stream()
        .filter(SingleVersionScan::isHandshakeOK)
        .map(SingleVersionScan::getProtocolVersion)
        .min(Comparator.comparingInt(TlsProtocolVersion::valueAsInt));
  }

  public Optional<TlsProtocolVersion> getHighestVersionSupported() {
    return scanPerVersionMap.values()
        .stream()
        .filter(SingleVersionScan::isHandshakeOK)
        .map(SingleVersionScan::getProtocolVersion)
        .max(Comparator.comparingInt(TlsProtocolVersion::valueAsInt));
  }

  public Optional<String> getServerName() {
    return scanPerVersionMap.values()
        .stream()
        .map(SingleVersionScan::getServerName)
        .filter(Objects::nonNull)
        .findFirst();
  }

  public boolean isChainTrustedByJavaPlatform() {
    return scanPerVersionMap.values()
        .stream()
        .anyMatch(SingleVersionScan::isChainTrustedByJavaPlatform);
  }

  public boolean isHostNameMatchesCertificate() {
    return scanPerVersionMap.values()
        .stream()
        .anyMatch(SingleVersionScan::isHostNameMatchesCertificate);
  }

}
