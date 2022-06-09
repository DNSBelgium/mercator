package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.ProtocolScanResult;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.InetSocketAddress;
@Setter
@Getter
@ToString(callSuper = true)
public class SSL2ScanResult extends ProtocolScanResult {

  @Setter
  @Getter
  private ServerHello serverHello;

  public static SSL2ScanResult failed(InetSocketAddress socketAddress, String errorMessage) {
    SSL2ScanResult scanResult = new SSL2ScanResult();
    scanResult.setProtocolVersion(TlsProtocolVersion.SSL_2);
    scanResult.setAddress(socketAddress);
    scanResult.setConnectOK(false);
    scanResult.setHandshakeOK(false);
    scanResult.setErrorMessage(errorMessage);
    scanResult.setPeerVerified(false);
    return scanResult;
  }

}
