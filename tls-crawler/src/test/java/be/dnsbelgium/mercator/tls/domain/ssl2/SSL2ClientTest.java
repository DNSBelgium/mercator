package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.ProtocolScanResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.ssl2.SSL2CipherSuite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

//@Disabled(value = "Only for local testing, needs internet access and depends on external state")
public class SSL2ClientTest {

  private static final Logger logger = getLogger(SSL2ClientTest.class);

  @Test
  public void ssl2_supported() throws InterruptedException {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2ScanResult scanResult = client.connect("www.chefxpo.be");
    logger.info("scanResult = {}", scanResult);
    logger.info("serverHello = {}", scanResult.getServerHello());
    ServerHello serverHello = scanResult.getServerHello();
    assertThat(serverHello).isNotNull();
    assertThat(serverHello.isSessionIdHit()).isFalse();
    assertThat(serverHello.getCertificateType()).isEqualTo(1);
    assertThat(serverHello.getVersionSelectedByServer()).isEqualTo(new byte[]{0, 2});
    assertThat(serverHello.getListSupportedCipherSuites()).hasSize(2);
    assertThat(serverHello.getConnectionId()).hasSize(16);
    assertThat(scanResult.getErrorMessage()).isNull();
    assertThat(scanResult.getIpAddress()).isNotNull();
    assertThat(scanResult.getServerName()).isEqualTo("www.chefxpo.be");
    assertThat(scanResult.isConnectOK()).isTrue();
    assertThat(scanResult.isHandshakeOK()).isTrue();
    assertThat(scanResult.getSelectedCipherSuite()).isEqualTo("SSL_CK_RC4_128_WITH_MD5");
    assertThat(scanResult.getSelectedProtocol()).isEqualTo("SSLv2");
  }

  @Test
  public void connectionRefused() {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2ScanResult scanResult = client.connect("localhost", 400);
    logger.info("scanResult = {}", scanResult);
    logger.info("scanResult.getErrorMessage() = {}", scanResult.getErrorMessage());
    assertThat(scanResult.isConnectOK()).isFalse();
    assertThat(scanResult.isHandshakeOK()).isFalse();
    assertThat(scanResult.getServerHello()).isNull();
    assertThat(scanResult.getSelectedProtocol()).isNull();
    assertThat(scanResult.getSelectedCipherSuite()).isNull();
    assertThat(scanResult.getServerHello()).isNull();
    assertThat(scanResult.getErrorMessage()).isEqualTo("Connection refused");
  }

  @Test
  public void connectionReset() throws InterruptedException {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2ScanResult scanResult = client.connect("google.be");
    logger.info("scanResult = {}", scanResult);
    assertThat(scanResult.isConnectOK()).isTrue();
    assertThat(scanResult.getServerHello()).isNull();
    // google.be actually replies with a TLSv1 Record Layer message
    // 0x15 (Alert) 0x0301 (TLS 1.0) 0x0002 (length 2) 0x02 (Level 2 = Fatal) 0x46 (Protocol Version)
    // 0x 15 03 01 00 02 02 46
    // But our SSLv2 decoders don't understand it correctly
    assertThat(scanResult.isHandshakeOK()).isFalse();
    assertThat(scanResult.getServerHello()).isNull();
    assertThat(scanResult.getSelectedProtocol()).isNull();
    assertThat(scanResult.getSelectedCipherSuite()).isNull();
  }

  @Test
  public void connectionTimedOut() {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2ScanResult scanResult = client.connect("google.be", 400);
    logger.info("scanResult = {}", scanResult);
    assertThat(scanResult.isConnectOK()).isFalse();
    assertThat(scanResult.isHandshakeOK()).isFalse();
    assertThat(scanResult.getServerHello()).isNull();
    assertThat(scanResult.getSelectedProtocol()).isNull();
    assertThat(scanResult.getSelectedCipherSuite()).isNull();
    assertThat(scanResult.getErrorMessage()).isEqualTo(ProtocolScanResult.CONNECTION_TIMED_OUT);
    logger.info("scanResult.getErrorMessage = {}", scanResult.getErrorMessage());
  }

  @Test
  @Disabled(value = "Takes over 5 seconds")
  public void checkAllCiphers() {
    String hostName = "www.anido.be";
    // It seems like this server always replies with these two ciphers in its ServerHello
    // regardless of what we send in ClientHello
    List<SSL2CipherSuite> expectedInServerHello = List.of(SSL_CK_RC4_128_WITH_MD5, SSL_CK_DES_192_EDE3_CBC_WITH_MD5);
    for (SSL2CipherSuite cipherSuite : SSL2CipherSuite.values()) {
      logger.info("Testing ClientHello with only one cipherSuite = {}", cipherSuite);
      List<SSL2CipherSuite> actualInServerHello = getSupportedCiphers(hostName, cipherSuite);
      assertThat(actualInServerHello).isEqualTo(expectedInServerHello);
    }
  }

  List<SSL2CipherSuite> getSupportedCiphers(String hostName, SSL2CipherSuite cipherSuiteInClientHello) {
    SSL2Client client = SSL2Client.with(cipherSuiteInClientHello);
    try {
      ServerHello serverHello = client.connect(hostName).getServerHello();
      logger.info("serverHello = {}", serverHello);
      return serverHello.getListSupportedCipherSuites();
    } catch (InterruptedException e) {
      logger.info("Testing {} for {} (SSLv2) => {}", hostName, cipherSuiteInClientHello, e.getMessage());
      return List.of();
    }
  }

}
