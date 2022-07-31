package be.dnsbelgium.mercator.tls.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static be.dnsbelgium.mercator.tls.metrics.MetricName.CACHE_RATE_LIMITER;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class RateLimiter {

  private final float delayFactor;
  private final long minDelayMs;
  private final long maxDelayMs;

  private final Cache<String, Instant> sleepUntilPerIp;

  private static final Logger logger = getLogger(RateLimiter.class);

  @Autowired
  public RateLimiter(
      MeterRegistry meterRegistry,
      @Value("${rate.limiter.max.cache.size:20_000}") long maxCacheSize,
      @Value("${rate.limiter.delay.factor:0.9}") float delayFactor,
      @Value("${rate.limiter.min.delay.ms:10}") long minDelayMs,
      @Value("${rate.limiter.max.delay.ms:500}") long maxDelayMs) {
    this.delayFactor = delayFactor;
    this.minDelayMs = minDelayMs;
    this.maxDelayMs = maxDelayMs;
    this.sleepUntilPerIp = Caffeine.newBuilder()
        .maximumSize(maxCacheSize)
        .expireAfterWrite(maxDelayMs, TimeUnit.MILLISECONDS)
        .build();
    CaffeineCacheMetrics.monitor(meterRegistry, sleepUntilPerIp, CACHE_RATE_LIMITER);
    logger.info("Created RateLimiter with maxCacheSize={}, delayFactor={}, minDelayMs={}, maxDelayMs={}",
        maxCacheSize, delayFactor, minDelayMs, maxDelayMs);
  }

  // The delay is computed based on the time it took last time to access the IP
  // Inspired by https://heritrix.readthedocs.io/en/latest/configuring-jobs.html#politeness
  private Duration computeDelay(Duration lastDuration) {
    if (lastDuration == null) {
      return Duration.ZERO;
    }
    float delay = lastDuration.toMillis() * delayFactor;
    if (delay > maxDelayMs) {
      return Duration.ofMillis(maxDelayMs);
    }
    if (delay < minDelayMs) {
      return Duration.ofMillis(minDelayMs);
    }
    return Duration.ofMillis((long) delay);
  }

  public void registerDuration(InetSocketAddress inetSocketAddress, Duration duration) {
    if (inetSocketAddress.isUnresolved()) {
      return;
    }
    String ipAddress = inetSocketAddress.getAddress().getHostAddress();
    registerDuration(ipAddress, duration);
  }

  public void registerDuration(String ipAddress, Duration duration) {
    Duration delay = computeDelay(duration);
    Instant until = now().plus(delay);
    if (logger.isDebugEnabled()) {
      logger.debug("Last connection to {} took {} => Block for {} until {}", ipAddress, duration, delay, until);
    }
    sleepUntilPerIp.put(ipAddress, until);
  }

  public long milliSecondsToWait(String ipAddress) {
    Instant sleepUntil = sleepUntilPerIp.getIfPresent(ipAddress);
    if (sleepUntil == null) {
      return 0;
    }
    Instant now = now();
    if (sleepUntil.isBefore(now)) {
      logger.debug("ip={} => No need to sleep. until={} is in the past", ipAddress, sleepUntil);
      return 0;
    }
    long millis = ChronoUnit.MILLIS.between(now, sleepUntil);
    logger.debug("ip={} => sleep {} ms until {}", ipAddress, millis, sleepUntil);
    return millis;
  }

  public void sleepIfNecessaryFor(InetSocketAddress inetSocketAddress) {
    if (inetSocketAddress.isUnresolved()) {
      return;
    }
    String ipAddress = inetSocketAddress.getAddress().getHostAddress();
    sleepIfNecessaryFor(ipAddress);
  }


  public void sleepIfNecessaryFor(String ipAddress) {
    long millis = milliSecondsToWait(ipAddress);
    if (millis == 0) {
      return;
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ignore) {
    }
  }

  public long estimatedCacheSize() {
    return sleepUntilPerIp.estimatedSize();
  }

}
