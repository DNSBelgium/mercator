package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.Certificate;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.ScanResult;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.TlsScanResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class TlsScanResultRepositoryTest {

  private static final Logger logger = getLogger(TlsScanResultRepositoryTest.class);

  @Autowired private CertificateRepository certificateRepository;
  @Autowired private TlsScanResultRepository tlsScanResultRepository;
  @Autowired private ScanResultRepository scanResultRepository;

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "tls_crawler");
  }

  @Test
  void save() {
    Certificate certificate = Certificate.builder()
        .sha256fingerprint("12345")
        .build();
    certificateRepository.save(certificate);
    logger.info("certificate = {}", certificate);

    ScanResult scanResult = ScanResult.builder()
        .serverName("dnsbelgium.be")
        .connectOk(true)
        .highestVersionSupported("TLS 1.3")
        .lowestVersionSupported("TLS 1.2")
        .supportTls_1_3(true)
        .supportTls_1_2(true)
        .supportTls_1_1(false)
        .supportTls_1_0(false)
        .supportSsl_3_0(false)
        .supportSsl_2_0(false)
        .errorTls_1_1("No can do")
        .errorTls_1_0("Go away")
        .errorSsl_3_0("Why?")
        .errorSsl_2_0("Protocol error")
        .ip("10.20.30.40")
        .crawlTimestamp(ZonedDateTime.now())
        .build();

    TlsScanResult tlsScanResult = TlsScanResult.builder()
        .scanResult(scanResult)
        .domainName("dns.be")
        .hostName("www.dns.be")
        .visitId(UUID.randomUUID())
        .crawlTimestamp(ZonedDateTime.now())
        .build();

    scanResultRepository.save(scanResult);
    tlsScanResultRepository.save(tlsScanResult);
    logger.info("AFTER: tlsScanResult = {}", tlsScanResult);

  }

}
