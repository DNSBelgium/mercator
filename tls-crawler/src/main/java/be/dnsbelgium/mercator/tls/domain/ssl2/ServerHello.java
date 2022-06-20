package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.codec.binary.Hex;

import java.util.List;

@Data
public class ServerHello {

  private final boolean sessionIdHit;
  private final int certificateType; // 1 = X.509 Certificate
  private final byte[] versionSelectedByServer;  // 2 bytes
  @ToString.Exclude
  private final byte[] certificate;
  private final List<SSL2CipherSuite> listSupportedCipherSuites;
  @ToString.Exclude
  private final byte[] connectionId;

  public ServerHello(boolean sessionIdHit,
                     int certificateType,
                     byte[] versionSelectedByServer,
                     byte[] certificate,
                     List<SSL2CipherSuite> listSupportedCipherSuites,
                     byte[] connectionId) {
    this.sessionIdHit = sessionIdHit;
    this.certificateType = certificateType;
    this.versionSelectedByServer = versionSelectedByServer;
    this.certificate = certificate;
    this.listSupportedCipherSuites = listSupportedCipherSuites;
    this.connectionId = connectionId;
  }

  @ToString.Include
  public int certificateLength() {
    return certificate.length;
  }

  public int getCipherSpecLength() {
    return listSupportedCipherSuites.size() * 3;
  }

  public String selectedVersion() {
    TlsProtocolVersion version = TlsProtocolVersion.from(versionSelectedByServer);
    if (version != null) {
      return version.getName();
    }
    return "0x" + Hex.encodeHexString(versionSelectedByServer);
  }
}
