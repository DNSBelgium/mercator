package be.dnsbelgium.mercator.tls.domain;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class RateLimiterTest {

  private static final Logger logger = getLogger(RateLimiterTest.class);

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  private RateLimiter makeRateLimiter(long maxCacheSize, float delayFactor, long minDelayMs, long maxDelayMs) {
    return new RateLimiter(meterRegistry, maxCacheSize, delayFactor, minDelayMs, maxDelayMs);
  }

  @Test
  public void initiallyNoDelay() {
    RateLimiter rateLimiter = makeRateLimiter(100,1, 10, 20);
    long delayMs = rateLimiter.milliSecondsToWait("10.20.30.40");
    logger.info("delayMs = {}", delayMs);
    assertThat(delayMs).isEqualTo(0);
  }

  @Test
  public void whenAlreadyAccessedApplyMinimumDelay() {
    RateLimiter rateLimiter = makeRateLimiter(100,1, 155, 2000);
    String ipAddress = "10.20.30.40";
    rateLimiter.registerDuration(ipAddress, Duration.ofMillis(50));
    long delayMs = rateLimiter.milliSecondsToWait(ipAddress);
    logger.info("delayMs = {}", delayMs);
    assertThat(delayMs).isBetween(100L, 200L);
  }

  @Test
  public void whenAlreadyAccessedApplyMaximumDelay() {
    RateLimiter rateLimiter = makeRateLimiter(20_000,1, 155, 2000);
    String ipAddress = "10.20.30.40";
    rateLimiter.registerDuration(ipAddress, Duration.ofMillis(4000));
    long delayMs = rateLimiter.milliSecondsToWait(ipAddress);
    logger.info("delayMs = {}", delayMs);
    assertThat(delayMs).isBetween(1990L, 2010L);
  }

  @Test
  public void applyDelayFactor() {
    RateLimiter rateLimiter = makeRateLimiter(100,4, 155, 2000);
    String ipAddress = "10.20.30.40";
    // 4 * 30 = 120 < 155
    rateLimiter.registerDuration(ipAddress, Duration.ofMillis(30));
    long delayMs = rateLimiter.milliSecondsToWait(ipAddress);
    assertThat(delayMs).isBetween(140L, 160L);

    // 4 * 50 = 200 => between 155 and 2000
    rateLimiter.registerDuration(ipAddress, Duration.ofMillis(50));
    delayMs = rateLimiter.milliSecondsToWait(ipAddress);
    assertThat(delayMs).isBetween(180L, 220L);

    // 4 * 600 = 2400 > 2000
    rateLimiter.registerDuration(ipAddress, Duration.ofMillis(600));
    delayMs = rateLimiter.milliSecondsToWait(ipAddress);
    assertThat(delayMs).isEqualTo(2000);
  }

  @Test
  public void whenMaxSizeReachedThenIpEvicted() {
    RateLimiter rateLimiter = makeRateLimiter(100,2, 10, 250);
    String ip = "10.20.30.40";
    rateLimiter.registerDuration(ip, Duration.ofMillis(123));
    //
    assertThat(rateLimiter.milliSecondsToWait(ip)).isBetween(240L, 260L);
    for (int i=1; i<=2000; i++) {
      rateLimiter.registerDuration("1.2.3." + i, Duration.ofMillis(100));
    }
    // Test could be flaky
    // see javadoc of com.github.benmanes.caffeine.cache.Caffeine.maximumSize
    // Note that the cache may evict an entry before this limit is exceeded or temporarily exceed the threshold while evicting.
    logger.info("rateLimiter.estimatedCacheSize = {}", rateLimiter.estimatedCacheSize());
    assertThat(rateLimiter.milliSecondsToWait(ip)).isEqualTo(0);
  }

  @Test
  public void ramUsage() {
    // just to get an idea of the RAM usage of the RateLimiter
    long bytesBefore = Runtime.getRuntime().totalMemory();
    long entries = 500_000;
    RateLimiter rateLimiter = makeRateLimiter(entries, 1, 10, 20_000);
    for (int i=1; i<=entries; i++) {
      // does not have to be a valid IP address
      String ip = "ip" + i;
      rateLimiter.registerDuration(ip, Duration.ofMillis(100));
    }
    logger.info("rateLimiter.estimatedCacheSize = {}", rateLimiter.estimatedCacheSize());
    assertThat(rateLimiter.estimatedCacheSize()).isBetween(entries - 10_000, entries + 10_000L);
    long bytesAfter = Runtime.getRuntime().totalMemory();
    logger.info("Before = {} bytes = {} MB", bytesBefore, bytesBefore/(1024*1024));
    logger.info("After  = {} bytes = {} MB", bytesAfter, bytesAfter/(1024*1024));
  }


}