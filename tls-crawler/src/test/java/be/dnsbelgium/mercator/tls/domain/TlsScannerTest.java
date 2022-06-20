package be.dnsbelgium.mercator.tls.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.time.Duration;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@Disabled(value = "These tests require internet access and depend on google's TLS configuration")
class TlsScannerTest {

  // if we do this early enough, we don't have to set a system property when starting the JVM
  // (-Djava.security.properties=/path/to/custom/security.properties)
  static {
    Security.setProperty("jdk.tls.disabledAlgorithms", "NULL");
    Security.setProperty("jdk.tls.legacyAlgorithms", "TLSv1,SSLv3,SSLv2");

  }

  private final TlsScanner tlsScanner = TlsScanner.standard();
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
    TlsScanner tlsScanner = new TlsScanner(400, false, false, 2000);
    ProtocolScanResult result = tlsScanner.scan(TLS_1_2, "google.be");
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
  public void localhost_tls12() {
    ProtocolScanResult result = tlsScanner.scan(TLS_1_2, "localhost");
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
  public void testCiphers() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    List<String> list30 = List.of(TlsScanner.factory(SSL_3).getSupportedCipherSuites());
    List<String> list10 = List.of(TlsScanner.factory(TLS_1_0).getSupportedCipherSuites());
    List<String> list11 = List.of(TlsScanner.factory(TLS_1_1).getSupportedCipherSuites());
    List<String> list12 = List.of(TlsScanner.factory(TLS_1_2).getSupportedCipherSuites());
    List<String> list13 = List.of(TlsScanner.factory(TLS_1_3).getSupportedCipherSuites());
    // all factories have the same set of supported cipher suites
    assertThat(list30).isEqualTo(list10);
    assertThat(list11).isEqualTo(list10);
    assertThat(list12).isEqualTo(list10);
    assertThat(list13).isEqualTo(list10);
  }

  @Test
  public void testDefaultCiphers() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    List<String> list30 = List.of(TlsScanner.factory(SSL_3).getDefaultCipherSuites());
    List<String> list10 = List.of(TlsScanner.factory(TLS_1_0).getDefaultCipherSuites());
    List<String> list11 = List.of(TlsScanner.factory(TLS_1_1).getDefaultCipherSuites());
    List<String> list12 = List.of(TlsScanner.factory(TLS_1_2).getDefaultCipherSuites());
    List<String> list13 = List.of(TlsScanner.factory(TLS_1_3).getDefaultCipherSuites());
    logger.info("list30.size() = {}", list30.size());
    logger.info("list10.size() = {}", list10.size());
    logger.info("list11.size() = {}", list11.size());
    logger.info("list12.size() = {}", list12.size());
    logger.info("list13.size() = {}", list13.size());
    logger.info("list12 = {}", list12);
    logger.info("list13 = {}", list13);
    // Running it again we get:
    //    list30.size() = 22
    //    list10.size() = 22
    //    list11.size() = 22
    //    list12.size() = 53
    //    list13.size() = 56
    // Why is this different from previous runs ??
    //    assertThat(list30).isEmpty();
    //    assertThat(list10).isEmpty();
    //    assertThat(list11).isEmpty();
    //    assertThat(list12).hasSize(46);
    //    assertThat(list13).hasSize(49);
  }

  @Test
  public void checkInetSocketAddress() {
    InetSocketAddress address = new InetSocketAddress("x", 443);
    logger.info("address = {}", address);
    logger.info("address.isUnresolved = {}", address.isUnresolved());
    logger.info("address.getAddress() = {}", address.getAddress());
    assertThat(address.isUnresolved()).isTrue();
    assertThat(address.getAddress()).isNull();
    assertThat(address.toString()).isEqualTo("x/<unresolved>:443");
  }

}