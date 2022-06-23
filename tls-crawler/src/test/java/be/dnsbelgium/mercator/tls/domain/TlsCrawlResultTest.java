package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion.*;
import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;
import static org.assertj.core.api.Assertions.assertThat;

class TlsCrawlResultTest {

  @Test
  public void lowestVersionSupported() {
    TlsCrawlResult tlsCrawlResult = new TlsCrawlResult(true);
    var address = new InetSocketAddress("example.com", 443);
    var tls13 = ProtocolScanResult.of(TLS_1_3, address);
    var tls12 = ProtocolScanResult.of(TLS_1_2, address);
    var tls11 = ProtocolScanResult.of(TLS_1_1, address);
    var tls10 = ProtocolScanResult.of(TLS_1_0, address);
    var sslv3 = ProtocolScanResult.of(SSL_3, address);
    var sslv2 = ProtocolScanResult.of(SSL_2, address);
    tlsCrawlResult.add(tls13);
    tlsCrawlResult.add(tls12);
    tlsCrawlResult.add(tls11);
    tlsCrawlResult.add(tls10);
    tlsCrawlResult.add(sslv3);
    tlsCrawlResult.add(sslv2);

    tls13.setHandshakeOK(false);
    tls12.setHandshakeOK(false);
    tls11.setHandshakeOK(true);
    tls10.setHandshakeOK(true);
    sslv3.setHandshakeOK(false);
    sslv2.setHandshakeOK(false);

    assertThat(tlsCrawlResult.getLowestVersionSupported()).hasValue(TLS_1_0);
    assertThat(tlsCrawlResult.getHighestVersionSupported()).hasValue(TLS_1_1);

    tls11.setHandshakeOK(false);
    assertThat(tlsCrawlResult.getLowestVersionSupported()).hasValue(TLS_1_0);
    assertThat(tlsCrawlResult.getHighestVersionSupported()).hasValue(TLS_1_0);

    String lowest = tlsCrawlResult.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    System.out.println("lowest = " + lowest);

    tls10.setHandshakeOK(false);
    assertThat(tlsCrawlResult.getLowestVersionSupported()).isEmpty();
    assertThat(tlsCrawlResult.getHighestVersionSupported()).isEmpty();

    lowest = tlsCrawlResult.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    System.out.println("lowest = " + lowest);

    sslv2.setHandshakeOK(true);
    assertThat(tlsCrawlResult.getLowestVersionSupported()).hasValue(SSL_2);
    assertThat(tlsCrawlResult.getHighestVersionSupported()).hasValue(SSL_2);

    sslv3.setHandshakeOK(true);
    assertThat(tlsCrawlResult.getLowestVersionSupported()).hasValue(SSL_2);
    assertThat(tlsCrawlResult.getHighestVersionSupported()).hasValue(SSL_3);

    tls13.setHandshakeOK(true);
    assertThat(tlsCrawlResult.getLowestVersionSupported()).hasValue(SSL_2);
    assertThat(tlsCrawlResult.getHighestVersionSupported()).hasValue(TLS_1_3);
  }

  @Test
  public void getCertificateChain() throws CertificateException, IOException {
    TlsCrawlResult tlsCrawlResult = new TlsCrawlResult(true);
    assertThat(tlsCrawlResult.getCertificateChain())
        .withFailMessage("Empty TlsCrawlResult should have no certificate chain").isEmpty();
    assertThat(tlsCrawlResult.getPeerCertificate()).isEmpty();
    var protocolScanResult = buildProtocolScanResult();
    List<CertificateInfo> chain = makeChain();
    protocolScanResult.setCertificateChain(chain);
    // asking tlsCrawlResult for the chain should yield the chain of protocolScanResult
    tlsCrawlResult.add(protocolScanResult);
    assertThat(tlsCrawlResult.getCertificateChain()).hasValue(chain);
    assertThat(tlsCrawlResult.getPeerCertificate()).hasValue(chain.get(0));
  }

  private List<CertificateInfo> makeChain() throws CertificateException, IOException {
    CertificateInfo caCertificate   = CertificateInfo.from(readTestCertificate("globalsign-ca.pem"));
    CertificateInfo leafCertificate = CertificateInfo.from(readTestCertificate("dnsbelgium.be.pem"));
    leafCertificate.setSignedBy(caCertificate);
    return List.of(leafCertificate, caCertificate);
  }

  private ProtocolScanResult buildProtocolScanResult() {
    return ProtocolScanResult.of(TLS_1_0, new InetSocketAddress("example.com", 443));
  }

}