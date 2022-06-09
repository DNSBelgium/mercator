package be.dnsbelgium.mercator.tls.domain;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Represents the result of scanning a host name for one specific <code>{@link TlsProtocolVersion}</code>
 */
@Data
@ToString
public class ProtocolScanResult {

  public final static String CONNECTION_TIMED_OUT = "Connection timed out";
  public final static String CONNECTION_REFUSED   = "Connection refused";
  public final static String CONNECTION_RESET     = "Connection reset";

  public static ProtocolScanResult of(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
    ProtocolScanResult protocolScanResult = new ProtocolScanResult();
    protocolScanResult.setProtocolVersion(protocolVersion);
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


  private TlsProtocolVersion protocolVersion;

  private boolean peerVerified;

  private String serverName;
  private String ipAddress;

  private boolean connectOK;
  private boolean handshakeOK;
  private String selectedCipherSuite;
  private String selectedProtocol;

  private String errorMessage;
  private String peerPrincipal;

  // TODO set certificate properties
  boolean selfSignedCertificate;
  boolean certificateExpired;
  boolean certificateTooEarly;

  private Duration scanDuration;

}
