package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents the result of scanning a host name for one specific <code>{@link TlsProtocolVersion}</code>
 */
@Data
public class ProtocolScanResult {

  private final TlsProtocolVersion protocolVersion;

  private boolean peerVerified;

  private String serverName;
  private String ipAddress;

  private boolean connectOK;
  private boolean handshakeOK;
  private String selectedCipherSuite;
  private String selectedProtocol;

  private String errorMessage;
  private String peerPrincipal;

  private CertificateInfo peerCertificate;
  private List<CertificateInfo> certificateChain;

  private boolean chainTrustedByJavaPlatform;
  private boolean hostNameMatchesCertificate;

  private Duration scanDuration = Duration.ZERO;

  public final static String CONNECTION_TIMED_OUT = "Connection timed out";
  public final static String CONNECTION_REFUSED   = "Connection refused";
  public final static String CONNECTION_RESET     = "Connection reset";

  private static final Logger logger = getLogger(ProtocolScanResult.class);

  protected ProtocolScanResult(TlsProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public static ProtocolScanResult of(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
    ProtocolScanResult protocolScanResult = new ProtocolScanResult(protocolVersion);
    protocolScanResult.setAddress(socketAddress);
    return protocolScanResult;
  }

  public void setAddress(InetSocketAddress socketAddress) {
    this.serverName = socketAddress.getHostString();
    if (socketAddress.isUnresolved()) {
      this.ipAddress = null;
    } else {
      this.ipAddress = socketAddress.getAddress().getHostAddress();
    }
  }


  public boolean hasCertificateChain() {
    return certificateChain != null && !certificateChain.isEmpty();
  }
}
