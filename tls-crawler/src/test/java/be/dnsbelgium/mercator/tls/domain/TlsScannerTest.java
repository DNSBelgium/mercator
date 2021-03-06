package be.dnsbelgium.mercator.tls.domain;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.Security;
import java.time.Duration;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@Disabled(value = "These tests require internet access and depends on the google.be TLS configuration (could change anytime)")
class TlsScannerTest {

  // if we do this early enough, we don't have to set a system property when starting the JVM
  // (-Djava.security.properties=/path/to/custom/security.properties)
  static {
    Security.setProperty("jdk.tls.disabledAlgorithms", "NULL");
    Security.setProperty("jdk.tls.legacyAlgorithms", "");
  }

  private final static TlsScanner tlsScanner = TlsScanner.standard();
  private static final Logger logger = getLogger(TlsScannerTest.class);

  @Test
  public void sslv3_protocol_alert() {
    ProtocolScanResult result = tlsScanner.scan(SSL_3, "google.be");
    logger.info("result = {}", result);
    assertThat(result.getIpAddress()).isNotNull();
    assertThat(result.isConnectOK()).isTrue();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    // No security properties set => "No appropriate protocol (protocol is disabled or cipher suites are inappropriate)"
    // With security properties => Received fatal alert: protocol_version
    assertThat(result.getErrorMessage()).isEqualTo("Received fatal alert: protocol_version");
    assertThat(result.getPeerPrincipal()).isNull();
    assertThat(result.getScanDuration()).isGreaterThan(Duration.ofNanos(1));
  }

  @Test
  public void connectionReset() {
    ProtocolScanResult result = tlsScanner.scan(SSL_2, "google.be");
    logger.info("result = {}", result);
    assertThat(result.getIpAddress()).isNotNull();
    assertThat(result.isConnectOK()).isTrue();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    // No security properties set => "No appropriate protocol (protocol is disabled or cipher suites are inappropriate)"
    // With security properties  => Received fatal alert: protocol_version
    assertThat(result.getErrorMessage()).isNotBlank();
    assertThat(result.getErrorMessage()).isEqualTo(ProtocolScanResult.CONNECTION_RESET);
    assertThat(result.getPeerPrincipal()).isNull();
    assertThat(result.getScanDuration()).isGreaterThan(Duration.ofNanos(1));
  }

  // TODO: start TLS server instead of relying on google and others
  // Do it in a separate test class ??
  //

  @Test
  public void een_be_ssl3() {
    // without setting soTimeOut, this takes around 20 seconds and results in SSLHandshakeException: Received fatal alert: protocol_version
    // with readTimeOut of 5 seconds, it results in connectOK=true, handshakeOK=false , errorMessage=Read timed out
    TlsScanner tlsScanner = new TlsScanner(
        new DefaultHostnameVerifier(),
        false, true, 5000, 4_000);
    ProtocolScanResult result = tlsScanner.scan(SSL_3, "een.be");
    logger.info("result.getScanDuration = {}", result.getScanDuration());
    assertThat(result.getScanDuration()).isLessThan(Duration.ofSeconds(6));
  }

  @Test
  public void renovations() {
    // 1a-renovations.be
    // chrome says: NET::ERR_CERT_COMMON_NAME_INVALID
    ProtocolScanResult result = tlsScanner.scanForProtocol(
        TLS_1_0, new InetSocketAddress("1a-renovations.be", 443));
    logger.info("result = {}", result);
  }

  @Test
  public void woonoutlet07_url() throws IOException {
    String hostname = "woonoutlet07.be";
    URL url = new URL("https://" + hostname);
    try {
      String content = "" + url.getContent();
      logger.info("content = {}", content);
      // javax.net.ssl.SSLHandshakeException: No subject alternative DNS name matching woonoutlet07.be found.
    } catch (SSLHandshakeException e) {
      logger.info("SSLHandshakeException", e);
    }
  }

    @Test
  public void google_be() {
    for (TlsProtocolVersion version : List.of(TLS_1_0, TLS_1_1, TLS_1_2, TLS_1_3)) {
      ProtocolScanResult result = tlsScanner.scan(version, "google.be");
      logger.info("result = {}", result);
      assertThat(result.getIpAddress()).isNotNull();
      assertThat(result.isConnectOK()).isTrue();
      assertThat(result.isHandshakeOK()).isTrue();

      if (version == TLS_1_0 || version == TLS_1_1) {
        assertThat(result.getSelectedCipherSuite()).isEqualTo("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA");
      }
      if (version == TLS_1_2) {
        assertThat(result.getSelectedCipherSuite()).isEqualTo("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
      }
      if (version == TLS_1_3) {
        assertThat(result.getSelectedCipherSuite()).isEqualTo("TLS_AES_256_GCM_SHA384");
      }
      assertThat(result.getSelectedProtocol()).isEqualTo(version.getName());
      assertThat(result.getErrorMessage()).isNull();
      assertThat(result.getPeerPrincipal()).isEqualTo("CN=*.google.be");
      assertThat(result.getScanDuration()).isGreaterThan(Duration.ofMillis(5));
    }
  }

  @Test
  public void connectionTimedOut() {
    TlsScanner tlsScanner = new TlsScanner(new DefaultHostnameVerifier(), false, false, 2000, 3000);
    ProtocolScanResult result = tlsScanner.scan(TLS_1_2, "google.be", 400);
    logger.info("result = {}", result);
    assertThat(result.getIpAddress()).isNotNull();
    assertThat(result.isConnectOK()).isFalse();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    assertThat(result.getErrorMessage()).isEqualTo(ProtocolScanResult.CONNECTION_TIMED_OUT);
    assertThat(result.getPeerPrincipal()).isNull();
    logger.info("result.getScanDuration = {}", result.getScanDuration());
  }

  @Test
  public void connection_refused() {
    ProtocolScanResult result = tlsScanner.scan(TLS_1_2, "localhost", 9745);
    logger.info("result = {}", result);
    assertThat(result.isConnectOK()).isFalse();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    assertThat(result.getErrorMessage()).isEqualTo("Connection refused");
    assertThat(result.getPeerPrincipal()).isNull();
    assertThat(result.getIpAddress()).isEqualTo("127.0.0.1");
    assertThat(result.getServerName()).isEqualTo("localhost");
  }

  @Test
  public void isUnresolved_InetSocketAddress() {
    InetSocketAddress address = new InetSocketAddress("x", 443);
    logger.info("address = {}", address);
    logger.info("address.isUnresolved = {}", address.isUnresolved());
    logger.info("address.getAddress() = {}", address.getAddress());
    assertThat(address.isUnresolved()).isTrue();
    assertThat(address.getAddress()).isNull();
    assertThat(address.toString()).isEqualTo("x/<unresolved>:443");
  }

}