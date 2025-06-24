package be.dnsbelgium.mercator.tls.domain.certificates;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.*;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.certificates.Certificate.convertBigIntegerToHexString;
import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class CertificateTest {

  private static final Logger logger = getLogger(CertificateTest.class);

  @Test
  public void dnsbelgium_be() throws CertificateException, IOException {
      X509Certificate certificate = readTestCertificate("dnsbelgium.be.pem");
      Certificate info = Certificate.from(certificate);
      logger.info("info = {}", info);
      logger.info("info = {}", info.prettyString());
      assertThat(info.getIssuer()).contains("CN=GlobalSign Extended Validation CA - SHA256 - G3,O=GlobalSign nv-sa,C=BE");
      assertThat(info.getSubject()).contains("CN=dnsbelgium.be,O=Domaine Name Registration België VZW,STREET=Philipssite 5  bus 13,L=Leuven,ST=Vlaams-Brabant,C=BE");
      assertThat(info.getPublicKeySchema()).isEqualTo("RSA");
      assertThat(info.getPublicKeyLength()).isEqualTo(2048);
      assertThat(info.getNotBefore()).isEqualTo("2021-12-16T09:46:13Z");
      assertThat(info.getNotAfter()).isEqualTo("2023-01-17T09:46:13Z");
      assertThat(info.getSerialNumberHex()).isEqualTo("21:40:5d:69:cb:87:3f:c1:88:88:e8:6e");
      assertThat(info.getVersion()).isEqualTo(3);
      assertThat(info.getSignatureHashAlgorithm()).isEqualTo("SHA256withRSA");
      assertThat(info.getSha256Fingerprint()).isEqualTo("0aa3423012f58713ffcff9aefe875eb308c53305a713128e41d80f3bc45a5aff");
  }

  @Test
  public void subjectAlternativeNames_DnsBelgium_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("dnsbelgium.be.pem");
    List<String> subjectAlternativeNames = Certificate.getSubjectAlternativeNames(certificate);
    logger.info("subjectAlternativeNames = {}", subjectAlternativeNames);
    assertThat(subjectAlternativeNames).containsOnly("dnsbelgium.be", "production.dnsbelgium.be", "www.dnsbelgium.be");
  }

  // TODO: test self-signed
  // TODO: test EV certificate
  // TODO: test OV certificate

  @Test
  public void blackanddecker_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("blackanddecker.be.pem");
    Certificate info = Certificate.from(certificate);
    logger.info("info = {}", info);
    logger.info("info = {}", info.prettyString());
    assertThat(info.getSha256Fingerprint()).isEqualTo("fb051996220fa119022b25ce0d476725c6711f9142001801c9437af0f6017739");
    assertThat(info.getIssuer()).isEqualTo("CN=DigiCert TLS RSA SHA256 2020 CA1,O=DigiCert Inc,C=US");
    assertThat(info.getSubject()).isEqualTo("CN=www.blackanddecker.com,O=Stanley Black & Decker\\, Inc.,L=New Britain,ST=Connecticut,C=US");
    assertThat(info.getPublicKeySchema()).isEqualTo("EC");
    assertThat(info.getPublicKeyLength()).isEqualTo(256);
    assertThat(info.getNotBefore()).isEqualTo("2022-11-21T00:00:00Z");
    assertThat(info.getNotAfter()).isEqualTo("2023-11-21T23:59:59Z");
    assertThat(info.getSerialNumberHex()).isEqualTo("0c:03:5e:1e:91:26:8b:8a:9d:cd:c8:46:03:6e:54:fe");
    assertThat(info.getVersion()).isEqualTo(3);
    assertThat(info.getSignatureHashAlgorithm()).isEqualTo("SHA256withRSA");
  }

  @Test
  public void cll_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("cll.be.pem");
    Certificate peerCertificate = Certificate.from(certificate);
    logger.info("peerCertificate = {}", peerCertificate);
    // matches with what python based ssl-crawler found.
    assertThat(peerCertificate.getVersion()).isEqualTo(3);
    assertThat(peerCertificate.getSerialNumberHex()).isEqualTo("59:6e:fa:96:d7:00:9e:4d:30:e1:92:6e:c3:5d:a8:28");
    assertThat(peerCertificate.getPublicKeySchema()).isEqualTo("RSA");
    assertThat(peerCertificate.getPublicKeyLength()).isEqualTo(2048);
    assertThat(peerCertificate.getNotBefore()).isEqualTo("2022-01-26T00:00:00Z");
    assertThat(peerCertificate.getNotAfter()) .isEqualTo("2023-01-26T23:59:59Z");
    assertThat(peerCertificate.getIssuer()) .isEqualTo("CN=Gandi Standard SSL CA 2,O=Gandi,L=Paris,ST=Paris,C=FR");
    assertThat(peerCertificate.getSubject()).isEqualTo("CN=www.cll.be");
    assertThat(peerCertificate.getSignatureHashAlgorithm()).isEqualTo("SHA256withRSA");
    assertThat(peerCertificate.getSha256Fingerprint()).isEqualTo("402514abe77794fc7c1d1b86b00e4f89a343c9755862fc050b27ed0488086af1");
    assertThat(peerCertificate.getSubjectAlternativeNames()).containsExactly("www.cll.be", "cll.be");
  }


  @Test
  public void subjectAlternativeNames_blackanddecker_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("blackanddecker.be.pem");
    List<String> subjectAlternativeNames = Certificate.getSubjectAlternativeNames(certificate);
    logger.info("subjectAlternativeNames = {}", subjectAlternativeNames);
    assertThat(subjectAlternativeNames).contains("www.vidmar.com", "www.protoindustrial.com", "americanpride.dewalt.com");
  }

  @Test
  public void convertBigIntegerToHexStringTest() {
    BigInteger bigint = new BigInteger("5050505");
    String hexString = convertBigIntegerToHexString(bigint);
    assertThat(hexString).isEqualTo("4d:10:89");
  }

  @Test
  public void convertBigIntegerToHexStringNull(){
    String hexString = convertBigIntegerToHexString(null);
    assertThat(hexString).isEqualTo(null);
  }
}