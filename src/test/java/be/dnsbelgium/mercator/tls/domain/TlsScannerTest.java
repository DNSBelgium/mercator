package be.dnsbelgium.mercator.tls.domain;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion.*;
import static be.dnsbelgium.mercator.tls.domain.TlsScanner.DEFAULT_CONNECT_TIME_OUT_MS;
import static be.dnsbelgium.mercator.tls.domain.TlsScanner.DEFAULT_READ_TIME_OUT_MS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@Disabled(value = "These tests require internet access and depends on the google.be TLS configuration (could change anytime)")
@ExtendWith(MockitoExtension.class)
class TlsScannerTest {

  // if we do this early enough, we don't have to set a system property when starting the JVM
  // (-Djava.security.properties=/path/to/custom/security.properties)
  static {
    TlsScanner.allowOldAlgorithms();
  }

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Mock
  RateLimiter rateLimiter;

  private static final Logger logger = getLogger(TlsScannerTest.class);

  @Test
  public void sslv3_protocol_alert() {
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    SingleVersionScan result = tlsScanner.scan(SSL_3, "google.be");
    logger.info("result = {}", result);
    assertThat(result.getIpAddress()).isNotNull();
    assertThat(result.isConnectOK()).isTrue();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    // No security properties set => "No appropriate protocol (protocol is disabled or cipher suites are inappropriate)"
    // With security properties => Received fatal alert: protocol_version
    assertThat(result.getErrorMessage()).contains("Received fatal alert: protocol_version");
    assertThat(result.getPeerPrincipal()).isNull();
    assertThat(result.getScanDuration()).isGreaterThan(Duration.ofNanos(1));
  }

  @Test
  public void connectionReset() {
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    SingleVersionScan result = tlsScanner.scan(SSL_2, "google.be");
    logger.info("result = {}", result);
    assertThat(result.getIpAddress()).isNotNull();
    assertThat(result.isConnectOK()).isTrue();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    // No security properties set => "No appropriate protocol (protocol is disabled or cipher suites are inappropriate)"
    // With security properties  => Received fatal alert: protocol_version
    assertThat(result.getErrorMessage()).isNotBlank();
    assertThat(result.getErrorMessage()).isEqualTo(SingleVersionScan.CONNECTION_RESET);
    assertThat(result.getPeerPrincipal()).isNull();
    assertThat(result.getScanDuration()).isGreaterThan(Duration.ofNanos(1));
  }

  // TODO: start TLS server instead of relying on google and others
  // Do it in a separate test class ??
  //
  TlsScanner makeTlsScanner(int connectTimeOutMilliSeconds, int readTimeOutMilliSeconds) {
    return new TlsScanner(
        new DefaultHostnameVerifier(),
        rateLimiter,
        meterRegistry,
        false, true, connectTimeOutMilliSeconds, readTimeOutMilliSeconds
        );

  }


  @Test
  public void een_be_ssl3() {
    // without setting soTimeOut, this takes around 20 seconds and results in SSLHandshakeException: Received fatal alert: protocol_version
    // with readTimeOut of 5 seconds, it results in connectOK=true, handshakeOK=false , errorMessage=Read timed out
    TlsScanner tlsScanner = makeTlsScanner(2000, 3000);
    SingleVersionScan result = tlsScanner.scan(SSL_3, "een.be");
    logger.info("result.getScanDuration = {}", result.getScanDuration());
    assertThat(result.getScanDuration()).isLessThan(Duration.ofSeconds(6));
  }

  @Test
  public void renovations() {
    // 1a-renovations.be
    // chrome says: NET::ERR_CERT_COMMON_NAME_INVALID
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    SingleVersionScan result = tlsScanner.scanForProtocol(
        TLS_1_0, new InetSocketAddress("1a-renovations.be", 443));
    logger.info("result = {}", result);
  }

    @Test
  public void google_be() {
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    for (TlsProtocolVersion version : List.of(TLS_1_0, TLS_1_1, TLS_1_2, TLS_1_3)) {
      SingleVersionScan result = tlsScanner.scan(version, "google.be");
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
    TlsScanner tlsScanner = makeTlsScanner(1000, 1500);
    SingleVersionScan result = tlsScanner.scan(TLS_1_2, "google.be", 400);
    logger.info("result = {}", result);
    assertThat(result.getIpAddress()).isNotNull();
    assertThat(result.isConnectOK()).isFalse();
    assertThat(result.isHandshakeOK()).isFalse();
    assertThat(result.getSelectedCipherSuite()).isNull();
    assertThat(result.getSelectedProtocol()).isNull();
    assertThat(result.getErrorMessage()).isEqualTo(SingleVersionScan.CONNECTION_TIMED_OUT);
    assertThat(result.getPeerPrincipal()).isNull();
    logger.info("result.getScanDuration = {}", result.getScanDuration());
  }

  @Test
  public void connection_refused() {
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    SingleVersionScan result = tlsScanner.scan(TLS_1_2, "localhost", 9745);
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

  @Test
  public void unresolved_InetSocketAddress() {
    InetSocketAddress address = new InetSocketAddress("x", 443);
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    SingleVersionScan result = tlsScanner.scanForProtocol(TLS_1_0, address);
    logger.info("result = {}", result);
  }

  @Test
  public void ssl3() {
    //InetSocketAddress address = new InetSocketAddress("93.88.240.42", 443);
    InetSocketAddress address = new InetSocketAddress("tecna.be", 443);
    TlsScanner tlsScanner = makeTlsScanner(DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
    SingleVersionScan result = tlsScanner.scanForProtocol(SSL_3, address);
    logger.info("result = {}", result);
    logger.info("result.isHandshakeOK = {}", result.isHandshakeOK());
    logger.info("result.getSelectedCipherSuite = {}", result.getSelectedCipherSuite());
  }


}