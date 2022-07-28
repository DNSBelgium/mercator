package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import be.dnsbelgium.mercator.tls.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToDoubleFunction;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class FullScanCache {

  private static final Logger logger = getLogger(FullScanCache.class);

  private final int minimumEntriesPerIp;
  private final double requiredRatio;

  // a normal HashMap would probably suffice since we use our own readWriteLock
  private final Map<String, CacheEntry> mapPerIp = new ConcurrentHashMap<>();

  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final boolean enabled;
  private final MeterRegistry meterRegistry;

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  public FullScanCache(
      @Value("${full.scan.cache.enabled:true}") boolean cacheEnabled,
      @Value("${full.scan.cache.minimum.entries.per.ip:10}") int minimumEntriesPerIp,
      @Value("${full.scan.cache.required.ratio:0.9}") double requiredRatio,
      MeterRegistry meterRegistry) {
    this.minimumEntriesPerIp = minimumEntriesPerIp;
    this.requiredRatio = requiredRatio;
    this.enabled = cacheEnabled;
    this.meterRegistry = meterRegistry;
    if (requiredRatio < 0.0 || requiredRatio > 1.0) {
      throw new IllegalArgumentException("requiredRatio was {} but should be between 0.0 and 1.0");
    }
    if (cacheEnabled) {
      logger.info("FullScanCache configured with minimumEntriesPerIp={} and requiredRatio={}", minimumEntriesPerIp, requiredRatio);
    } else {
      logger.warn("FullScanCache is DISABLED");
    }
    this.meterRegistry.gaugeMapSize(MetricName.GAUGE_SCANRESULT_CACHE_SIZE, Tags.empty(), mapPerIp);
    this.meterRegistry.gauge(MetricName.GAUGE_SCANRESULT_CACHE_DEEP_ENTRIES, Tags.empty(), this, FullScanCache::countDeepEntries);
  }

  public FullScanCache(
      int minimumEntriesPerIp,
      double requiredRatio) {
    this(true, minimumEntriesPerIp, requiredRatio, new SimpleMeterRegistry());
  }

  public Long countDeepEntries() {
    return mapPerIp.values().stream().mapToLong(cacheEntry -> cacheEntry.totalFullScanEntities).sum();
  }

  public static FullScanCache withDefaultSettings() {
    return new FullScanCache(10, 0.9);
  }

  public void evictEntriesOlderThan(Duration duration) {
    readWriteLock.writeLock().lock();
    try {
      Instant notBefore = Instant.now().minus(duration);
      logger.info("Evicting entries that older than {}, so after {}", duration, notBefore);
      int entriesBefore = mapPerIp.size();
      mapPerIp.entrySet().removeIf(e -> e.getValue().added.isBefore(notBefore));
      int entriesAfter = mapPerIp.size();
      logger.info("Eviction done. before cache had {} entries, now it has {} entries", entriesBefore, entriesAfter);
      meterRegistry.gauge(MetricName.GAUGE_SCANRESULT_CACHE_SIZE, entriesAfter);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public int size() {
    readWriteLock.readLock().lock();
    try {
      return mapPerIp.size();
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  public void add(FullScanEntity fullScanEntity) {
    add(Instant.now(), fullScanEntity);
  }

  public void add(Instant added, FullScanEntity fullScanEntity) {
    if (!enabled) {
      return;
    }
    if (fullScanEntity.getIp() == null) {
      // No need to cache when we could not find an IP
      return;
    }
    if (fullScanEntity.getId() == null) {
      // Makes no sense the cache a FullScanEntity that was not yet persisted
      logger.warn("Attempted to cache a FullScanEntity that has no id: {}", fullScanEntity);
      return;
    }
    logger.info("Adding to cache: IP = {} : fullScanEntity: {}", fullScanEntity.getIp(), fullScanEntity.summary());
    readWriteLock.writeLock().lock();
    String ip = fullScanEntity.getIp();
    try {
      CacheEntry entry = mapPerIp.get(ip);
      if (entry == null) {
        logger.info("First time we see this IP: {}", ip);
        mapPerIp.put(ip, CacheEntry.of(added, fullScanEntity));
      } else {
        if (fullScanEntity.getId().equals(entry.majority.getId())) {
          // given FullScanEntity already cached => do nothing
          return;
        }
        if (entry.serverNames.contains(fullScanEntity.getServerName())) {
          // only cache each serverName once
          return;
        }
        entry.serverNames.add(fullScanEntity.getServerName());
        String summary = fullScanEntity.summary();
        String majoritySummary = entry.majority.summary();
        if (StringUtils.equals(summary, majoritySummary)) {
          // new FullScanEntity matches with the majority
          entry.resultsInMajority++;
          entry.totalFullScanEntities++;
          logger.debug("new FullScanEntity matches with the majority: {}", entry);
        } else {
          // we found a deviant FullScanEntity => check if it became the majority
          if (entry.resultsInMajority > entry.totalFullScanEntities / 2 + 1) {
            // majority won't change
            entry.totalFullScanEntities++;
            logger.debug("we found a deviant FullScanEntity but majority config did not change: {}", entry);
          } else {
            // count entries in deviantFullScanEntities that have same summary
            long matchingEntries = entry.deviantFullScanEntities.stream().filter(p -> p.summary().equals(summary)).count();
            if (matchingEntries + 1 > entry.resultsInMajority) {
              // we have a new majority
              CacheEntry newEntry = entry.newMajority(added, fullScanEntity);
              logger.info("we have a new majority: {}", newEntry);
              mapPerIp.put(ip, newEntry);
            } else {
              logger.debug("we found a deviant FullScanEntity but majority config did not change: {}", entry);
              entry.totalFullScanEntities++;
            }
          }
        }
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public Optional<FullScanEntity> find(String ip) {
    if (!enabled) {
      return Optional.empty();
    }
    readWriteLock.readLock().lock();
    try {
      CacheEntry match = mapPerIp.get(ip);
      if (match == null) {
        logger.debug("No match for {}", ip);
        return cacheMiss();
      }
      if (match.totalFullScanEntities < minimumEntriesPerIp) {
        logger.debug("Not enough scans for this IP. required={} in-cache={}", minimumEntriesPerIp, match.totalFullScanEntities);
        return cacheMiss();
      }
      logger.debug("totalFullScanEntities={} resultsInMajority={}", match.totalFullScanEntities, match.resultsInMajority);
      double majorityRatio = (1.0 * match.resultsInMajority) / match.totalFullScanEntities;
      logger.debug("requiredRatio={} actual ratio={}", requiredRatio, majorityRatio);

      if (majorityRatio < requiredRatio) {
        logger.debug("Majority not strong enough: actual={} required={}", majorityRatio, requiredRatio);
        return cacheMiss();
      }
      meterRegistry.counter(MetricName.COUNTER_SCANRESULT_CACHE_HITS).increment();
      return Optional.of(match.majority);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  private Optional<FullScanEntity> cacheMiss() {
    meterRegistry.counter(MetricName.COUNTER_SCANRESULT_CACHE_MISSES).increment();
    return Optional.empty();
  }

  @AllArgsConstructor
  private static class CacheEntry {
    private FullScanEntity majority;

    private final List<FullScanEntity> deviantFullScanEntities = new ArrayList<>();
    private long totalFullScanEntities;
    private long resultsInMajority;
    private final String ip;

    private final Set<String> serverNames = new TreeSet<>();

    private final Instant added;

    private static CacheEntry of(Instant added, FullScanEntity fullScanEntity) {
      CacheEntry entry = new CacheEntry(fullScanEntity, 1, 1, fullScanEntity.getIp(), added);
      entry.serverNames.add(fullScanEntity.getServerName());
      return entry;
    }

    private CacheEntry newMajority(Instant added, FullScanEntity fullScanEntity) {
      String newSummary = fullScanEntity.summary();
      long matchingEntries = deviantFullScanEntities.stream().filter(p -> p.summary().equals(newSummary)).count() + 1;
      CacheEntry entry = new CacheEntry(fullScanEntity, totalFullScanEntities +1, matchingEntries, fullScanEntity.getIp(), added);
      entry.deviantFullScanEntities.clear();
      entry.deviantFullScanEntities.addAll(deviantFullScanEntities.stream().filter(r -> !r.summary().equals(newSummary)).toList());
      entry.deviantFullScanEntities.add(this.majority);
      entry.serverNames.addAll(this.serverNames);
      return entry;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", CacheEntry.class.getSimpleName() + "[", "]")
          .add("totalFullScanEntities=" + totalFullScanEntities)
          .add("resultsInMajority=" + resultsInMajority)
          .add("majority.summary=" + majority.summary())
          .add("ip='" + ip + "'")
          .toString();
    }

  }
}
