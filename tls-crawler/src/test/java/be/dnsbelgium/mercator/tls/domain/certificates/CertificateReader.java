package be.dnsbelgium.mercator.tls.domain.certificates;

import be.dnsbelgium.mercator.test.ResourceReader;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateReader {

  /**
   * Reads a certificate from the "/test-certificates/" folder
   * @param filename the name of the PEM file to read
   * @return an X509Certificate
   * @throws IOException when reading the file failed
   * @throws CertificateException when certifcate could not be loaded
   */
  public static X509Certificate readTestCertificate(String filename) throws IOException, CertificateException {
    try (InputStream is = ResourceReader.read("classpath:/test-certificates/" + filename)) {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      return (X509Certificate) fact.generateCertificate(is);
    }
  }

}
