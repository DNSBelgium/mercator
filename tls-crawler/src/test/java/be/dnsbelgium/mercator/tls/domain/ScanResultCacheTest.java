package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.ScanResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class ScanResultCacheTest {

  private final static String IP1 = "10.20.30.40";
  private final static String IP2 = "1.2.3.4";
  private final static String IP3 = "3.3.3.3";

  private Long idGenerator = 1L;

  private static final Logger logger = getLogger(ScanResultCacheTest.class);

  @Test
  public void whenMapIsEmptyThenWeFindNoResult() {
    ScanResultCache scanResultCache = ScanResultCache.withDefaultSettings();
    Optional<ScanResult> found = scanResultCache.find(IP1);
    assertThat(found).isEmpty();
  }

  @Test
  public void whenAllSameConfigThenWeFindEntry() {
    ScanResultCache scanResultCache = new ScanResultCache(3, 1.0);
    Optional<ScanResult> found = scanResultCache.find(IP1);
    assertThat(found).isEmpty();
    ScanResult result1 = makeScanResult(IP1, "example1.be");
    ScanResult result2 = makeScanResult(IP1, "example2.be");
    ScanResult result3 = makeScanResult(IP1, "example3.be");
    scanResultCache.add(result1);
    assertThat(scanResultCache.find(IP1)).isEmpty();
    scanResultCache.add(result2);
    assertThat(scanResultCache.find(IP1)).isEmpty();
    scanResultCache.add(result3);
    found = scanResultCache.find(IP1);
    logger.info("found = {}", found);
    assertThat(found).isNotEmpty();
    assertThat(found.get().summary()).isEqualTo(result1.summary());
  }

  @Test
  public void whenEntriesAreOldEvictRemovesThem() {
    ScanResultCache scanResultCache = new ScanResultCache(3, 1.0);
    ScanResult result1 = makeScanResult(IP1, "example1.be");
    ScanResult result2 = makeScanResult(IP2, "example2.be");
    ScanResult result3 = makeScanResult(IP3, "example3.be");
    Instant timestamp = Instant.now().minusMillis(100);
    scanResultCache.add(timestamp, result1);
    scanResultCache.add(timestamp, result2);
    scanResultCache.add(Instant.now().plusSeconds(5), result3);
    assertThat(scanResultCache.size()).isEqualTo(3);
    scanResultCache.evictEntriesOlderThan(Duration.ofMinutes(5));
    assertThat(scanResultCache.size()).isEqualTo(3);
    scanResultCache.evictEntriesOlderThan(Duration.ofMillis(2));
    assertThat(scanResultCache.size()).isEqualTo(1);
  }

  @Test
  public void whenRequiredRatioNotReachedThenNoMatch() {
    ScanResultCache scanResultCache = new ScanResultCache(2, 0.75);
    ScanResult result1 = ScanResult.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_first.be")
        .id(idGenerator++)
        .build();
    scanResultCache.add(result1);
    // minimumEntriesPerIp not reached
    assertThat(scanResultCache.find(IP1)).isEmpty();

    ScanResult result2 = ScanResult.builder()
        .ip(IP1)
        .supportTls_1_2(true)
        .serverName("tls12_deviant.be")
        .id(idGenerator++)
        .build();
    scanResultCache.add(result2);
    // minimumEntriesPerIp reached but ratio = 0.5 < 0.75
    assertThat(scanResultCache.find(IP1)).isEmpty();

    ScanResult result3 = ScanResult.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_second.be")
        .id(idGenerator++)
        .build();

    scanResultCache.add(result3);
    // ratio = 0.66 < 0.75
    assertThat(scanResultCache.find(IP1)).isEmpty();

    ScanResult result4 = ScanResult.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_third.be")
        .id(idGenerator++)
        .build();
    scanResultCache.add(result4);
    // ratio = 0.75 => OK
    Optional<ScanResult> found = scanResultCache.find(IP1);
    logger.info("found = {}", found);
    assertThat(found).isNotEmpty();
  }

  @Test
  public void whenAddingSameResultTwiceThenItIsAddedOnlyOnce() {
    ScanResultCache scanResultCache = new ScanResultCache(2, 0.75);
    ScanResult result1 = ScanResult.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_first.be")
        .build();
    scanResultCache.add(result1);
    // minimumEntriesPerIp not reached
    assertThat(scanResultCache.find(IP1)).isEmpty();
    scanResultCache.add(result1);
    // minimumEntriesPerIp still not reached
    assertThat(scanResultCache.find(IP1)).isEmpty();
  }

  private ScanResult makeScanResult(String ip, String serverName) {
    return ScanResult.builder()
        .ip(ip)
        .supportTls_1_3(true)
        .serverName(serverName)
        .id(idGenerator++)
        .build();
  }

}