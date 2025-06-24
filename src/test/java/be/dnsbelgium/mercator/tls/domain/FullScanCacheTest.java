package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.metrics.MetricName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static be.dnsbelgium.mercator.test.TestUtils.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class FullScanCacheTest {

  private final static String IP1 = "10.20.30.40";
  private final static String IP2 = "1.2.3.4";
  private final static String IP3 = "3.3.3.3";

  private static final Logger logger = getLogger(FullScanCacheTest.class);

  @Test
  public void whenMapIsEmptyThenWeFindNoResult() {
    FullScanCache fullScanCache = FullScanCache.withDefaultSettings();
    Optional<FullScanEntity> found = fullScanCache.find(IP1);
    assertThat(found).isEmpty();
  }

  @Test
  public void gaugeCacheSize() {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    FullScanCache fullScanCache = new FullScanCache(true, 3, 1.0, meterRegistry);
    Optional<Gauge> gauge = meterRegistry.get(MetricName.GAUGE_SCANRESULT_CACHE_DEEP_ENTRIES).gauges().stream().findFirst();
    assertThat(gauge).isPresent();
    assertThat(gauge.get().value()).isEqualTo(0);
    String ip = "10.20.30.40";
    fullScanCache.add(now(), makeFullScanEntity(ip, "abc.be"));
    assertThat(gauge.get().value()).isEqualTo(1);
    fullScanCache.add(now(), makeFullScanEntity(ip, "abcd.be"));
    assertThat(gauge.get().value()).isEqualTo(2);
    fullScanCache.add(now(), makeFullScanEntity(ip, "abcd.be"));
    assertThat(gauge.get().value()).isEqualTo(2);
    fullScanCache.add(now(), makeFullScanEntity(ip, "abc.be"));
  }

  @Test
  public void whenSameServerNameThenMinimumEntriesNotReached() {
    FullScanCache fullScanCache = new FullScanCache(2, 0.0);
    fullScanCache.add(now(), makeFullScanEntity(IP1, "example.be"));
    fullScanCache.add(now(), makeFullScanEntity(IP1, "example.be"));
    fullScanCache.add(now(), makeFullScanEntity(IP1, "example.be"));
    Optional<FullScanEntity> found = fullScanCache.find(IP1);
    assertThat(found).isEmpty();
  }

  @Test
  public void whenAllSameConfigThenWeFindEntry() {
    FullScanCache fullScanCache = new FullScanCache(3, 1.0);
    Optional<FullScanEntity> found = fullScanCache.find(IP1);
    assertThat(found).isEmpty();
    FullScanEntity result1 = makeFullScanEntity(IP1, "example1.be");
    FullScanEntity result2 = makeFullScanEntity(IP1, "example2.be");
    FullScanEntity result3 = makeFullScanEntity(IP1, "example3.be");
    fullScanCache.add(result1);
    assertThat(fullScanCache.find(IP1)).isEmpty();
    fullScanCache.add(result2);
    assertThat(fullScanCache.find(IP1)).isEmpty();
    fullScanCache.add(result3);
    found = fullScanCache.find(IP1);
    logger.info("fullScanCache found = {}", found);
    assertThat(found).isNotEmpty();
    assertThat(found.get().summary()).isEqualTo(result1.summary());
  }

  @Test
  public void whenEntriesAreOldEvictRemovesThem() {
    FullScanCache fullScanCache = new FullScanCache(3, 1.0);
    FullScanEntity result1 = makeFullScanEntity(IP1, "example1.be");
    FullScanEntity result2 = makeFullScanEntity(IP2, "example2.be");
    FullScanEntity result3 = makeFullScanEntity(IP3, "example3.be");
    Instant timestamp = now().minusMillis(100);
    fullScanCache.add(timestamp, result1);
    fullScanCache.add(timestamp, result2);
    fullScanCache.add(now().plusSeconds(5), result3);
    assertThat(fullScanCache.size()).isEqualTo(3);
    fullScanCache.evictEntriesOlderThan(Duration.ofMinutes(5));
    assertThat(fullScanCache.size()).isEqualTo(3);
    fullScanCache.evictEntriesOlderThan(Duration.ofMillis(2));
    assertThat(fullScanCache.size()).isEqualTo(1);
  }

  @Test
  public void whenRequiredRatioNotReachedThenNoMatch() {
    FullScanCache fullScanCache = new FullScanCache(2, 0.75);
    FullScanEntity result1 = FullScanEntity.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_first.be")
        .build();
    fullScanCache.add(result1);
    // minimumEntriesPerIp not reached
    assertThat(fullScanCache.find(IP1)).isEmpty();

    FullScanEntity result2 = FullScanEntity.builder()
        .ip(IP1)
        .supportTls_1_2(true)
        .serverName("tls12_deviant.be")
        .build();
    fullScanCache.add(result2);
    // minimumEntriesPerIp reached but ratio = 0.5 < 0.75
    assertThat(fullScanCache.find(IP1)).isEmpty();

    FullScanEntity result3 = FullScanEntity.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_second.be")
        .build();

    fullScanCache.add(result3);
    // ratio = 0.66 < 0.75
    assertThat(fullScanCache.find(IP1)).isEmpty();

    FullScanEntity result4 = FullScanEntity.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_third.be")
        .build();
    fullScanCache.add(result4);
    // ratio = 0.75 => OK
    Optional<FullScanEntity> found = fullScanCache.find(IP1);
    logger.info("found = {}", found);
    assertThat(found).isNotEmpty();
  }

  @Test
  public void whenAddingSameResultTwiceThenItIsAddedOnlyOnce() {
    FullScanCache fullScanCache = new FullScanCache(2, 0.75);
    FullScanEntity result1 = FullScanEntity.builder()
        .ip(IP1)
        .supportTls_1_3(true)
        .serverName("tls13_first.be")
        .build();
    fullScanCache.add(result1);
    // minimumEntriesPerIp not reached
    assertThat(fullScanCache.find(IP1)).isEmpty();
    fullScanCache.add(result1);
    // minimumEntriesPerIp still not reached
    assertThat(fullScanCache.find(IP1)).isEmpty();
  }

  private FullScanEntity makeFullScanEntity(String ip, String serverName) {
    return FullScanEntity.builder()
        .ip(ip)
        .supportTls_1_3(true)
        .serverName(serverName)
        .build();
  }

}