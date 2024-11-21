package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CertificateEntity;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class CertificateRepositoryTest {

  private static final Logger logger = getLogger(CertificateRepositoryTest.class);

  @Test
  void saveAllAttributes() {
    CertificateEntity certificateEntity = CertificateEntity.builder()
        .sha256fingerprint("12345678")
        .issuer("I am the issuer")
        .subject("I am the subject")
        .notBefore(TestUtils.now().minus(5, ChronoUnit.DAYS))
        .notAfter(TestUtils.now().plus(5, ChronoUnit.DAYS))
        .publicKeyLength(2048)
        .publicKeySchema("SHA-256")
        .serialNumberHex("70:ed:8d:46:88:9d:90:7f:0d:e5:04:1e")
        .subjectAltNames(List.of("abc.be", "xyz.com"))
        .signatureHashAlgorithm("")
        .version(3)
        .signedBySha256(null)
        .build();
    logger.info("certificateEntity = {}", certificateEntity);
    // TODO: call save and move to correct class
  }


  }
