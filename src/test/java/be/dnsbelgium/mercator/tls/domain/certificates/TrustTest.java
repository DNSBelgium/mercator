package be.dnsbelgium.mercator.tls.domain.certificates;

import be.dnsbelgium.mercator.tls.domain.RateLimiter;
import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import be.dnsbelgium.mercator.tls.domain.TlsScanner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class TrustTest {

  private static final Logger logger = getLogger(TrustTest.class);

  @Mock
  private RateLimiter rateLimiter;

  @Test
  public void testDefault() {
    Trust.defaultTrustManager();
    // TrustManagerFactory.getDefaultAlgorithm = PKIX
    // trustManager = sun.security.ssl.X509TrustManagerImpl@221a3fa4
    X509ExtendedTrustManager extendedTrustManager = (X509ExtendedTrustManager) Trust.defaultTrustManager();

    X509Certificate[] accepted = extendedTrustManager.getAcceptedIssuers();
    for (X509Certificate certificate : accepted) {
      logger.info("certificate = {}", certificate.getSubjectX500Principal());
    }

  }

  @Test
  public void check() {
    TlsScanner scanner = TlsScanner.standard(rateLimiter);
    // checkServerTrusted with ECDHE_RSA
    InetSocketAddress address = new InetSocketAddress("een.be", 443);
    SingleVersionScan singleVersionScan = scanner.scanForProtocol(TlsProtocolVersion.TLS_1_2, address);
    logger.info("singleVersionScan = {}", singleVersionScan);
  }

  @Test
  public void generateCertPath() throws NoSuchAlgorithmException, CertificateException, IOException, InvalidAlgorithmParameterException {
    X509Certificate[] chain = loadChain();
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    CertPath path = factory.generateCertPath(Arrays.asList(chain));
    X509Certificate[] accepted = Trust.defaultTrustManager().getAcceptedIssuers();

    PKIXBuilderParameters params = new PKIXBuilderParameters(
        Arrays.stream(accepted)
            .map(c -> new TrustAnchor(c, null))
            .collect(Collectors.toSet()),
        null);

    CertPathValidator validator = CertPathValidator.getInstance("PKIX");
    PKIXRevocationChecker rc = (PKIXRevocationChecker)validator.getRevocationChecker();
    rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL, PKIXRevocationChecker.Option.NO_FALLBACK));
    params.setRevocationEnabled(false);

    try {
      PKIXCertPathValidatorResult pathValidatorResult = (PKIXCertPathValidatorResult) validator.validate(path, params);
      TrustAnchor trustAnchor = pathValidatorResult.getTrustAnchor();
      X509Certificate trustAnchorCert = trustAnchor.getTrustedCert();
      logger.info("trustAnchor.getTrustedCert = {}", trustAnchorCert);

      String caName = pathValidatorResult.getTrustAnchor().getCAName();
      logger.info("caName = {}", caName);


    } catch (CertPathValidatorException e) {
      logger.info("e = {}", e.getMessage());
      logger.info("getSoftFailExceptions = {}", rc.getSoftFailExceptions());
    }

  }


  private X509Certificate[] loadChain() throws CertificateException, IOException {
    X509Certificate certificate1 = readTestCertificate("blackanddecker.be.pem");
    X509Certificate ca = readTestCertificate("digicert-ca.pem");
    return new X509Certificate[] { certificate1, ca };
  }

}