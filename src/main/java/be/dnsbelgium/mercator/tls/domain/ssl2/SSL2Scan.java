package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.InetSocketAddress;
@Setter
@Getter
@ToString(callSuper = true)
public class SSL2Scan extends SingleVersionScan {

  @Setter
  @Getter
  private ServerHello serverHello;

  public SSL2Scan() {
    super(TlsProtocolVersion.SSL_2);
  }

  public static SSL2Scan failed(InetSocketAddress socketAddress, String errorMessage) {
    SSL2Scan scan = new SSL2Scan();
    scan.setAddress(socketAddress);
    scan.setConnectOK(false);
    scan.setHandshakeOK(false);
    scan.setErrorMessage(errorMessage);
    scan.setPeerVerified(false);
    return scan;
  }

}
