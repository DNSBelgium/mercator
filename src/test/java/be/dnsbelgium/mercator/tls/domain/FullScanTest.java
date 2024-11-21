package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion.*;
import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;
import static org.assertj.core.api.Assertions.assertThat;

class FullScanTest {

  private static final Logger logger = LoggerFactory.getLogger(FullScanTest.class);

  @Test
  public void lowestVersionSupported() {
    FullScan fullScan = new FullScan(true);
    var address = new InetSocketAddress("example.com", 443);
    var tls13 = SingleVersionScan.of(TLS_1_3, address);
    var tls12 = SingleVersionScan.of(TLS_1_2, address);
    var tls11 = SingleVersionScan.of(TLS_1_1, address);
    var tls10 = SingleVersionScan.of(TLS_1_0, address);
    var sslv3 = SingleVersionScan.of(SSL_3, address);
    var sslv2 = SingleVersionScan.of(SSL_2, address);
    fullScan.add(tls13);
    fullScan.add(tls12);
    fullScan.add(tls11);
    fullScan.add(tls10);
    fullScan.add(sslv3);
    fullScan.add(sslv2);

    tls13.setHandshakeOK(false);
    tls12.setHandshakeOK(false);
    tls11.setHandshakeOK(true);
    tls10.setHandshakeOK(true);
    sslv3.setHandshakeOK(false);
    sslv2.setHandshakeOK(false);

    assertThat(fullScan.getLowestVersionSupported()).hasValue(TLS_1_0);
    assertThat(fullScan.getHighestVersionSupported()).hasValue(TLS_1_1);

    tls11.setHandshakeOK(false);
    assertThat(fullScan.getLowestVersionSupported()).hasValue(TLS_1_0);
    assertThat(fullScan.getHighestVersionSupported()).hasValue(TLS_1_0);

    String lowest = fullScan.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    logger.info("lowest = " + lowest);

    tls10.setHandshakeOK(false);
    assertThat(fullScan.getLowestVersionSupported()).isEmpty();
    assertThat(fullScan.getHighestVersionSupported()).isEmpty();

    lowest = fullScan.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    logger.info("lowest = " + lowest);

    sslv2.setHandshakeOK(true);
    assertThat(fullScan.getLowestVersionSupported()).hasValue(SSL_2);
    assertThat(fullScan.getHighestVersionSupported()).hasValue(SSL_2);

    sslv3.setHandshakeOK(true);
    assertThat(fullScan.getLowestVersionSupported()).hasValue(SSL_2);
    assertThat(fullScan.getHighestVersionSupported()).hasValue(SSL_3);

    tls13.setHandshakeOK(true);
    assertThat(fullScan.getLowestVersionSupported()).hasValue(SSL_2);
    assertThat(fullScan.getHighestVersionSupported()).hasValue(TLS_1_3);
  }

  @Test
  public void getCertificateChain() throws CertificateException, IOException {
    FullScan fullScan = new FullScan(true);
    assertThat(fullScan.getCertificateChain())
        .withFailMessage("Empty FullScan should have no certificate chain").isEmpty();
    assertThat(fullScan.getPeerCertificate()).isEmpty();
    SingleVersionScan singleVersionScan = SingleVersionScan.of(TLS_1_0, new InetSocketAddress("example.com", 443));
    List<Certificate> chain = makeChain();
    singleVersionScan.setCertificateChain(chain);
    // asking fullScan for the chain should yield the chain of singleVersionScan
    fullScan.add(singleVersionScan);
    assertThat(fullScan.getCertificateChain()).hasValue(chain);
    assertThat(fullScan.getPeerCertificate()).hasValue(chain.get(0));
  }

  private List<Certificate> makeChain() throws CertificateException, IOException {
    Certificate caCertificate   = Certificate.from(readTestCertificate("globalsign-ca.pem"));
    Certificate leafCertificate = Certificate.from(readTestCertificate("dnsbelgium.be.pem"));
    leafCertificate.setSignedBy(caCertificate);
    return List.of(leafCertificate, caCertificate);
  }

}