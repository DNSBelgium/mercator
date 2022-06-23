package be.dnsbelgium.mercator.tls.domain.certificates;

import be.dnsbelgium.mercator.test.ResourceReader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class CertificateInfoTest {

  private static final Logger logger = getLogger(CertificateInfoTest.class);

  @Test
  public void dnsbelgium_be() throws CertificateException, IOException {
      X509Certificate certificate = readTestCertificate("dnsbelgium.be.pem");
      CertificateInfo info = CertificateInfo.from(certificate);
      logger.info("info = {}", info);
      logger.info("info = {}", info.prettyString());
      assertThat(info.getIssuer()).contains("CN=GlobalSign Extended Validation CA - SHA256 - G3,O=GlobalSign nv-sa,C=BE");
      assertThat(info.getSubject()).contains("CN=dnsbelgium.be,O=Domaine Name Registration België VZW,STREET=Philipssite 5  bus 13,L=Leuven,ST=Vlaams-Brabant,C=BE");
      assertThat(info.getPublicKeySchema()).isEqualTo("RSA");
      assertThat(info.getPublicKeyLength()).isEqualTo(2048);
      assertThat(info.getNotBefore()).isEqualTo("2021-12-16T09:46:13Z");
      assertThat(info.getNotAfter()).isEqualTo("2023-01-17T09:46:13Z");
      assertThat(info.getSerialNumber()).isEqualTo("10290817708216518963613984878");
      assertThat(info.getVersion()).isEqualTo(3);
      assertThat(info.getSignatureHashAlgorithm()).isEqualTo("SHA256withRSA");
      assertThat(info.getSha256Fingerprint()).isEqualTo("0aa3423012f58713ffcff9aefe875eb308c53305a713128e41d80f3bc45a5aff");
  }

  @Test
  public void subjectAlternativeNames_DnsBelgium_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("dnsbelgium.be.pem");
    List<String> subjectAlternativeNames = CertificateInfo.getSubjectAlternativeNames(certificate);
    logger.info("subjectAlternativeNames = {}", subjectAlternativeNames);
    assertThat(subjectAlternativeNames).containsOnly("dnsbelgium.be", "production.dnsbelgium.be", "www.dnsbelgium.be");
  }

  // TODO: test self-signed
  // TODO : test EV certificate
  // TODO : test OV certificate

  @Test
  public void blackanddecker_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("blackanddecker.be.pem");
    CertificateInfo info = CertificateInfo.from(certificate);
    logger.info("info = {}", info);
    logger.info("info = {}", info.prettyString());
    assertThat(info.getSha256Fingerprint()).isEqualTo("ca20405088d49d4b0134d8e10467d34578ef0f664973d637c8686e9c9a00d39d");
    assertThat(info.getIssuer()).isEqualTo("CN=DigiCert SHA2 Secure Server CA,O=DigiCert Inc,C=US");
    assertThat(info.getSubject()).isEqualTo("CN=www.blackanddecker.com,O=Stanley Black & Decker\\, Inc.,L=New Britain,ST=Connecticut,C=US");
    assertThat(info.getPublicKeySchema()).isEqualTo("EC");
    assertThat(info.getPublicKeyLength()).isEqualTo(256);
    assertThat(info.getNotBefore()).isEqualTo("2021-12-17T00:00:00Z");
    assertThat(info.getNotAfter()).isEqualTo("2022-12-17T23:59:59Z");
    assertThat(info.getSerialNumber()).isEqualTo("17256317477817387864342631933386872678");
    assertThat(info.getVersion()).isEqualTo(3);
    assertThat(info.getSignatureHashAlgorithm()).isEqualTo("SHA256withRSA");
  }

  @Test
  public void subjectAlternativeNames_blackanddecker_be() throws CertificateException, IOException {
    X509Certificate certificate = readTestCertificate("blackanddecker.be.pem");
    List<String> subjectAlternativeNames = CertificateInfo.getSubjectAlternativeNames(certificate);
    logger.info("subjectAlternativeNames = {}", subjectAlternativeNames);
    assertThat(subjectAlternativeNames).contains("www.vidmar.com", "www.protoindustrial.com", "americanpride.dewalt.com");
  }

}