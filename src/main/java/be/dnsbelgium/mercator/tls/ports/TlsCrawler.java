package be.dnsbelgium.mercator.tls.ports;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.*;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.visits.CrawlerModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static be.dnsbelgium.mercator.tls.metrics.MetricName.COUNTER_VISITS_COMPLETED;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsCrawler implements CrawlerModule<TlsCrawlResult> {

  private final FullScanCache fullScanCache;
  private final TlsScanner tlsScanner;
  //private final TlsRepository tlsRepository;
  private final BlackList blackList;
  private final MeterRegistry meterRegistry;

  private static final Logger logger = getLogger(TlsCrawler.class);

  @Value("${tls.scanner.destination.port:443}") private int destinationPort;
  @Value("${tls.crawler.visit.apex:true}")      private boolean visitApex;
  @Value("${tls.crawler.visit.www:true}")       private boolean visitWww;
  @Value("${tls.crawler.allow.noop:false}")     private boolean allowNoop;

  @Autowired
  public TlsCrawler(
          TlsScanner tlsScanner,
          FullScanCache fullScanCache,
          //TlsRepository tlsRepository,
          BlackList blackList, MeterRegistry meterRegistry) {
      this.fullScanCache = fullScanCache;
      this.tlsScanner = tlsScanner;
      //this.tlsRepository = tlsRepository;
      this.blackList = blackList;
      this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  public void checkConfig() {
    logger.info("visitApex = tls.crawler.visit.apex = {}", visitApex);
    logger.info("visitWww  = tls.crawler.visit.www  = {}", visitWww);
    if (!visitApex && !visitWww) {
      logger.error("visitApex == visitWww == false => The TLS crawler will basically do nothing !!");
      if (!allowNoop) {
        logger.error("The TLS crawler will basically do nothing !!");
        logger.error("Set tls.crawler.allow.noop=false if this is really what you want.");
        throw new RuntimeException("visitApex == visitWww == allowNoop = false. \n" +
            "Set tls.crawler.allow.noop=false if this is really what you want");
      }
    }
  }

  public void addToCache(TlsCrawlResult tlsCrawlResult) {
    fullScanCache.add(Instant.now(), tlsCrawlResult.getFullScanEntity());
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
   * @param visitRequest the domain name to visit
   * @return the results of scanning the domain name
   */
  public TlsCrawlResult visit(String hostName, VisitRequest visitRequest) {
    logger.info("Crawling {}", visitRequest);
    InetSocketAddress address = new InetSocketAddress(hostName, destinationPort);

    if (!address.isUnresolved()) {
      String ip = address.getAddress().getHostAddress();
      Optional<FullScanEntity> resultFromCache = fullScanCache.find(ip);
      if (resultFromCache.isPresent()) {
        logger.debug("Found matching result in the cache. Now get certificates for {}", hostName);
        TlsProtocolVersion version = TlsProtocolVersion.of(resultFromCache.get().getHighestVersionSupported());
        SingleVersionScan singleVersionScan = (version != null) ? tlsScanner.scan(version, hostName) : null;
        return TlsCrawlResult.fromCache(hostName, visitRequest, resultFromCache.get(), singleVersionScan);
      }
    }
    FullScan fullScan = scanIfNotBlacklisted(address);
    return TlsCrawlResult.fromScan(hostName, visitRequest, fullScan);
  }

  private FullScan scanIfNotBlacklisted(InetSocketAddress address) {
    if (blackList.isBlacklisted(address)) {
      return FullScan.connectFailed(address, "IP address is blacklisted");
    }
    return tlsScanner.scan(address);
  }


  @Override
  public List<TlsCrawlResult> collectData(VisitRequest visitRequest) {
    List<TlsCrawlResult> results = new ArrayList<>();
    try {
      Threads.TLS.incrementAndGet();
      if (visitApex) {
        String hostName = visitRequest.getDomainName();
        TlsCrawlResult tlsCrawlResult = visit(hostName, visitRequest);
        results.add(tlsCrawlResult);
      }
      if (visitWww) {
        String hostName = "www." + visitRequest.getDomainName();
        TlsCrawlResult tlsCrawlResult = visit(hostName, visitRequest);
        results.add(tlsCrawlResult);
      }
      meterRegistry.counter(COUNTER_VISITS_COMPLETED).increment();
    } finally {
      Threads.TLS.decrementAndGet();
    }
    return results;
  }

  @Override
  public void save(List<?> collectedData) {
    collectedData.forEach(this::save);
  }

  public void save(Object item) {
    if (item instanceof TlsCrawlResult visit) {
      saveItem(visit);
    } else {
      logger.error("Cannot save item of type: {}", item.getClass().getName());
    }
  }


  @Override
  public void saveItem(TlsCrawlResult tlsCrawlResult) {
//    tlsRepository.persist(tlsCrawlResult);
  }

  @Override
  public void afterSave(List<?> collectedData) {
    for (Object object : collectedData) {
      if (object instanceof TlsCrawlResult tlsCrawlResult) {
        addToCache(tlsCrawlResult);
      }
    }
  }

  @Override
  public List<TlsCrawlResult> find(String visitId) {
    // TODO
    throw new NotImplementedException("TODO");
  }

  @Override
  public void createTables() {
    //tlsRepository.createTablesTls();
  }
}
