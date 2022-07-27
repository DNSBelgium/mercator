package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.ssl2.SSL2CipherSuite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@Disabled(value = "Only for local testing, needs internet access and depends on external state")
public class SSL2ClientTest {

  private static final Logger logger = getLogger(SSL2ClientTest.class);

  @Test
  public void ssl2_supported() throws InterruptedException {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2Scan ssl2Scan = client.connect("www.chefxpo.be");
    logger.info("ssl2Scan = {}", ssl2Scan);
    logger.info("serverHello = {}", ssl2Scan.getServerHello());
    ServerHello serverHello = ssl2Scan.getServerHello();
    assertThat(serverHello).isNotNull();
    assertThat(serverHello.isSessionIdHit()).isFalse();
    assertThat(serverHello.getCertificateType()).isEqualTo(1);
    assertThat(serverHello.getVersionSelectedByServer()).isEqualTo(new byte[]{0, 2});
    assertThat(serverHello.getListSupportedCipherSuites()).hasSize(2);
    assertThat(serverHello.getConnectionId()).hasSize(16);
    assertThat(ssl2Scan.getErrorMessage()).isNull();
    assertThat(ssl2Scan.getIpAddress()).isNotNull();
    assertThat(ssl2Scan.getServerName()).isEqualTo("www.chefxpo.be");
    assertThat(ssl2Scan.isConnectOK()).isTrue();
    assertThat(ssl2Scan.isHandshakeOK()).isTrue();
    assertThat(ssl2Scan.getSelectedCipherSuite()).isEqualTo("SSL_CK_RC4_128_WITH_MD5");
    assertThat(ssl2Scan.getSelectedProtocol()).isEqualTo("SSLv2");
  }

  @Test
  public void connectionRefused() {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2Scan ssl2Scan = client.connect("localhost", 400);
    logger.info("ssl2Scan = {}", ssl2Scan);
    logger.info("ssl2Scan.getErrorMessage() = {}", ssl2Scan.getErrorMessage());
    assertThat(ssl2Scan.isConnectOK()).isFalse();
    assertThat(ssl2Scan.isHandshakeOK()).isFalse();
    assertThat(ssl2Scan.getServerHello()).isNull();
    assertThat(ssl2Scan.getSelectedProtocol()).isNull();
    assertThat(ssl2Scan.getSelectedCipherSuite()).isNull();
    assertThat(ssl2Scan.getServerHello()).isNull();
    assertThat(ssl2Scan.getErrorMessage()).isEqualTo("Connection refused");
  }

  @Test
  public void connectionReset() throws InterruptedException {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2Scan scan = client.connect("google.be");
    logger.info("SSL2Scan = {}", scan);
    assertThat(scan.isConnectOK()).isTrue();
    assertThat(scan.getServerHello()).isNull();
    // google.be actually replies with a TLSv1 Record Layer message
    // 0x15 (Alert) 0x0301 (TLS 1.0) 0x0002 (length 2) 0x02 (Level 2 = Fatal) 0x46 (Protocol Version)
    // 0x 15 03 01 00 02 02 46
    // But our SSLv2 decoders don't understand it correctly
    assertThat(scan.isHandshakeOK()).isFalse();
    assertThat(scan.getServerHello()).isNull();
    assertThat(scan.getSelectedProtocol()).isNull();
    assertThat(scan.getSelectedCipherSuite()).isNull();
  }

  @Test
  public void connectionTimedOut() {
    SSL2Client client = SSL2Client.withAllKnownCiphers();
    SSL2Scan scan = client.connect("google.be", 400);
    logger.info("SSL2Scan = {}", scan);
    assertThat(scan.isConnectOK()).isFalse();
    assertThat(scan.isHandshakeOK()).isFalse();
    assertThat(scan.getServerHello()).isNull();
    assertThat(scan.getSelectedProtocol()).isNull();
    assertThat(scan.getSelectedCipherSuite()).isNull();
    assertThat(scan.getErrorMessage()).isEqualTo(SingleVersionScan.CONNECTION_TIMED_OUT);
    logger.info("fullScanEntity.getErrorMessage = {}", scan.getErrorMessage());
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
