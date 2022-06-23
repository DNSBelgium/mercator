package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.Certificate;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class CertificateRepositoryTest {

  private static final Logger logger = getLogger(CertificateRepositoryTest.class);

  @Autowired
  private CertificateRepository certificateRepository;

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "tls_crawler");
  }

  @Test
  void save() {
    Certificate certificate = Certificate.builder().sha256fingerprint("12345").build();
    certificateRepository.save(certificate);
    logger.info("certificate = {}", certificate);
  }

  @Test
  @Commit
  void saveAllAttributes() {
    Certificate certificate = Certificate.builder()
        .sha256fingerprint("12345678")
        .issuer("I am the issuer")
        .subject("I am the subject")
        .notBefore(Instant.now().minus(5, ChronoUnit.DAYS))
        .notAfter(Instant.now().plus(5, ChronoUnit.DAYS))
        .publicKeyLength(2048)
        .publicKeySchema("SHA-256")
        .serialNumber("4545454545645645454545645")
        .subjectAltNames(List.of("abc.be", "xyz.com"))
        .signatureHashAlgorithm("")
        .version(3)
        .signedBySha256(null)
        .build();
    certificateRepository.save(certificate);
    logger.info("certificate = {}", certificate);

    Optional<Certificate> found = certificateRepository.findBySha256fingerprint("12345678");
    assertThat(found).isPresent();
  }


  }
