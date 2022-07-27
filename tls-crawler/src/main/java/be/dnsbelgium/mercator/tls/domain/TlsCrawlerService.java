package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CertificateEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CrawlResultEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.CertificateRepository;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.FullScanRepository;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.CrawlResultRepository;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TlsCrawlerService {

  private final TlsScanner tlsScanner;

  private final FullScanCache fullScanCache;

  private final BlackList blackList;

  private final CertificateRepository certificateRepository;
  private final CrawlResultRepository crawlResultRepository;
  private final FullScanRepository fullScanRepository;

  private static final Logger logger = getLogger(TlsCrawlerService.class);

  private final int destinationPort;

  @Autowired
  public TlsCrawlerService(
      @Value("${tls.scanner.destination.port:443}") int destinationPort,
      TlsScanner tlsScanner, FullScanCache fullScanCache, BlackList blackList,
      CertificateRepository certificateRepository, CrawlResultRepository crawlResultRepository,
      FullScanRepository fullScanRepository) {
    this.destinationPort = destinationPort;
    this.tlsScanner = tlsScanner;
    this.fullScanCache = fullScanCache;
    this.blackList = blackList;
    this.certificateRepository = certificateRepository;
    this.crawlResultRepository = crawlResultRepository;
    this.fullScanRepository = fullScanRepository;
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing TlsCrawlerService");
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
  public CrawlResult visit(VisitRequest visitRequest) {
    logger.info("Crawling {}", visitRequest);
    String hostName = visitRequest.getDomainName();
    InetSocketAddress address = new InetSocketAddress(hostName, destinationPort);

    if (!address.isUnresolved()) {
      String ip = address.getAddress().getHostAddress();
      Optional<FullScanEntity> resultFromCache = fullScanCache.find(ip);
      if (resultFromCache.isPresent()) {
        logger.debug("Found matching result in the cache. Now get certificates for {}", hostName);
        TlsProtocolVersion version = TlsProtocolVersion.of(resultFromCache.get().getHighestVersionSupported());
        SingleVersionScan singleVersionScan = (version != null) ? tlsScanner.scan(version, hostName) : null;
        return CrawlResult.fromCache(visitRequest, resultFromCache.get(), singleVersionScan);
      }
    }
    FullScan fullScan = scanIfNotBlacklisted(address);
    return CrawlResult.fromScan(visitRequest, fullScan);
  }

  @Transactional
  public void persist(CrawlResult crawlResult) {
    logger.debug("Persisting crawlResult");
    CrawlResultEntity crawlResultEntity = crawlResult.convertToEntity();
    if (crawlResult.isFresh()) {
      fullScanRepository.save(crawlResult.getFullScanEntity());
    }
    saveCertificates(crawlResult.getCertificateChain());
    crawlResultRepository.save(crawlResultEntity);
  }

  private FullScan scanIfNotBlacklisted(InetSocketAddress address) {
    if (blackList.isBlacklisted(address)) {
      return FullScan.connectFailed(address, "IP address is blacklisted");
    }
    return tlsScanner.scan(address);
  }

  private void saveCertificates(Optional<List<Certificate>> chain) {
    if (chain.isPresent()) {
      // We have to save the chain in reversed order because of the foreign keys
      List<Certificate> reversed = new ArrayList<>(chain.get());
      Collections.reverse(reversed);
      for (Certificate certificate : reversed) {
        // We always call save, let Hibernate 2nd level cache do its magic
        // Note: this could over-write pre-existing certificates,
        // but we assume the attributes will remain the same
        CertificateEntity certificateEntity = certificate.asEntity();
        certificateRepository.save(certificateEntity);
      }
    }
  }

}
