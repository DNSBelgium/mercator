package be.dnsbelgium.mercator.tls.ports;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.tls.domain.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static be.dnsbelgium.mercator.tls.metrics.MetricName.COUNTER_VISITS_COMPLETED;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsCrawler implements ItemProcessor<VisitRequest, TlsCrawlResult> {

  private final FullScanCache fullScanCache;
  private final TlsScanner tlsScanner;
  private final BlackList blackList;
  private final MeterRegistry meterRegistry;

  private static final Logger logger = getLogger(TlsCrawler.class);

  @Value("${tls.scanner.destination.port:443}")
  private int destinationPort;
  @Value("${tls.crawler.prefixes:,www.}")
  private Set<String> prefixes;

  @Autowired
  public TlsCrawler(
      TlsScanner tlsScanner,
      FullScanCache fullScanCache,
      BlackList blackList, MeterRegistry meterRegistry) {
    this.fullScanCache = fullScanCache;
    this.tlsScanner = tlsScanner;
    this.blackList = blackList;
    this.meterRegistry = meterRegistry;
  }

  @SuppressWarnings("unused") // TODO
  public void addToCache(TlsVisit tlsVisit) {
    fullScanCache.add(Instant.now(), tlsVisit.getFullScanEntity());
  }

  @Scheduled(fixedRate = 15, initialDelay = 15, timeUnit = TimeUnit.MINUTES)
  public void clearCacheScheduled() {
    logger.info("clearCacheScheduled: evicting entries older than 4 hours");
    // every 15 minutes we remove all FullScanEntity objects older than 4 hours from the cache
    fullScanCache.evictEntriesOlderThan(Duration.ofHours(4));
  }

  /**
   * Either visit the domain name or get the result from the cache.
   * Does NOT save anything in the database
   *
   * @param visitRequest the domain name to visit
   * @return the results of scanning the domain name
   */
  public TlsVisit visit(VisitRequest visitRequest, String prefix) {
    logger.info("Crawling {}", visitRequest);
    String hostName = prefix + visitRequest.getDomainName();
    InetSocketAddress address = new InetSocketAddress(hostName, destinationPort);

    if (!address.isUnresolved()) {
      String ip = address.getAddress().getHostAddress();
      Optional<FullScanEntity> resultFromCache = fullScanCache.find(ip);
      if (resultFromCache.isPresent()) {
        logger.debug("Found matching result in the cache. Now get certificates for {}", hostName);
        TlsProtocolVersion version = TlsProtocolVersion.of(resultFromCache.get().getHighestVersionSupported());
        SingleVersionScan singleVersionScan = null;
        if (version != null) {
          singleVersionScan = tlsScanner.scan(version, hostName);
        }
        return TlsVisit.fromCache(
                visitRequest.getVisitId(),
                visitRequest.getDomainName(),
                hostName,
                Instant.now(),
                resultFromCache.get(),
                singleVersionScan);
      }
    }
    FullScan fullScan = scanIfNotBlacklisted(address);
    return TlsVisit.fromScan(
            visitRequest.getVisitId(),
            visitRequest.getDomainName(),
            hostName,
            Instant.now(),
            fullScan);
  }

  private FullScan scanIfNotBlacklisted(InetSocketAddress address) {
    if (blackList.isBlacklisted(address)) {
      return FullScan.connectFailed(address, "IP address is blacklisted");
    }
    return tlsScanner.scan(address);
  }


  @Override
  public TlsCrawlResult process(@NonNull VisitRequest visitRequest) throws Exception {
    try {
      Threads.TLS.incrementAndGet();
      return new TlsCrawlResult(
          visitRequest.getVisitId(),
          visitRequest.getDomainName(),
          prefixes.stream().map(prefix -> this.visit(visitRequest, prefix)).toList());
    } finally {
      meterRegistry.counter(COUNTER_VISITS_COMPLETED).increment();
      Threads.TLS.decrementAndGet();
    }
  }
}
