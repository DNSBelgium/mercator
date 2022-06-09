package be.dnsbelgium.mercator.tls.domain.ssl2;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class ClientHello {

  private final int maxSupportedVersion;

  // should be 32 bytes long according to https://www.rfc-editor.org/rfc/rfc2246.html
  @ToString.Exclude
  private final byte[] challengeData;

  // must have a length of either zero or 16
  @ToString.Exclude
  private final byte[] sessionId;

  private final List<SSL2CipherSuite> listSupportedCipherSuites;

  public ClientHello(int maxSupportedVersion,
                     List<SSL2CipherSuite> listSupportedCipherSuites,
                     byte[] sessionId,
                     byte[] challengeData) {
    this.maxSupportedVersion = maxSupportedVersion;
    this.listSupportedCipherSuites = listSupportedCipherSuites;
    this.sessionId = sessionId;
    this.challengeData = challengeData;
    if (sessionId.length != 0 && sessionId.length != 32) {
      throw new IllegalArgumentException("sessionId must be zero or 16 bytes long but length is " + sessionId.length);
    }
    if (challengeData.length != 32) {
      throw new IllegalArgumentException("challengeData must be 32 bytes long but length is " + challengeData.length);
    }
  }

}
