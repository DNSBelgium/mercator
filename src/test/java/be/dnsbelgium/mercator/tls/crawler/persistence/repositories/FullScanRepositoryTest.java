package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Instant;

import static org.slf4j.LoggerFactory.getLogger;

public class FullScanRepositoryTest {

  private static final Logger logger = getLogger(FullScanRepositoryTest.class);

  @Test
  public void save() {
    FullScanEntity fullScanEntity = FullScanEntity.builder()
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
        .crawlTimestamp(Instant.now())
        .build();
    logger.info("BEFORE fullScanEntity = {}", fullScanEntity);
  }

}
