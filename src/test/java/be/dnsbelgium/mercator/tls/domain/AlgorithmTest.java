package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.TrustAnythingTrustManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class AlgorithmTest {

  static {
    TlsScanner.allowOldAlgorithms();
  }

  private static final Logger logger = getLogger(AlgorithmTest.class);

  @Test
  public void testTlsAlgorithms() throws NoSuchAlgorithmException, KeyManagementException, IOException {
    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      if (version == TlsProtocolVersion.SSL_2) {
        // SSLSocketFactory does not support SSL 2.0
        continue;
      }
      SSLSocketFactory factory = TlsScanner.factory(version, new TrustAnythingTrustManager(), false);
      SSLSocket soc = (SSLSocket) factory.createSocket();
      soc.setEnabledProtocols(new String[]{version.getName()});
      String[] protocols = soc.getEnabledProtocols();
      List<String> enabledProtocols = new ArrayList<>(List.of(protocols));
      logger.info("version={} => enabledProtocols = {}", version, enabledProtocols);
      assertThat(enabledProtocols).containsExactly(version.getName());
    }
  }

  @Test
  void checkAlgorithms() throws IOException, NoSuchAlgorithmException {
    SSLContext sslContext = SSLContext.getDefault();
    String[] expected = new String[] {"TLSv1.3",  "TLSv1.2",  "TLSv1.1", "TLSv1", "SSLv3" };
    SSLSocketFactory factory = sslContext.getSocketFactory();
    SSLSocket soc = (SSLSocket) factory.createSocket();
    logger.info("before setEnabledProtocols => {}",  Arrays.toString(soc.getEnabledProtocols()));
    soc.setEnabledProtocols(expected);
    String[] protocols = soc.getEnabledProtocols();

    // we should actually test a real handshake
    // but this test already allows to quickly test the default behaviour of the JVM

    List<String> results = List.of(protocols);
    logger.info("after setEnabledProtocols => {}", results);
    assertThat(results).containsExactlyInAnyOrder(
        "TLSv1.3",
        "TLSv1.2",
        "TLSv1.1",
        "TLSv1",
        "SSLv3"
    );
  }

  @Test
  public void unsupportedProtocolThrowsException() throws NoSuchAlgorithmException, IOException {
    SSLContext sslContext = SSLContext.getDefault();
    SSLSocketFactory factory = sslContext.getSocketFactory();
    SSLSocket soc = (SSLSocket) factory.createSocket();
    Assertions.assertThrows(IllegalArgumentException.class, () -> soc.setEnabledProtocols(new String[] {"invalid"}));
  }




}
