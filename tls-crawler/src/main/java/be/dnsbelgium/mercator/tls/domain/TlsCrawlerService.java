package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.Certificate;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.ScanResult;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.TlsScanResult;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.CertificateRepository;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.ScanResultRepository;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.TlsScanResultRepository;
import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;
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

  private final ScanResultCache scanResultCache;

  private final BlackList blackList;

  private final CertificateRepository certificateRepository;
  private final TlsScanResultRepository tlsScanResultRepository;
  private final ScanResultRepository scanResultRepository;

  private static final Logger logger = getLogger(TlsCrawlerService.class);

  private final int destinationPort;

  @Autowired
  public TlsCrawlerService(
      @Value("${tls.scanner.destination.port:443}") int destinationPort,
      TlsScanner tlsScanner, ScanResultCache scanResultCache, BlackList blackList,
      CertificateRepository certificateRepository, TlsScanResultRepository tlsScanResultRepository,
      ScanResultRepository scanResultRepository) {
    this.destinationPort = destinationPort;
    this.tlsScanner = tlsScanner;
    this.scanResultCache = scanResultCache;
    this.blackList = blackList;
    this.certificateRepository = certificateRepository;
    this.tlsScanResultRepository = tlsScanResultRepository;
    this.scanResultRepository = scanResultRepository;
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing TlsCrawlerService");
  }

  @Scheduled(fixedRate = 15, initialDelay = 15, timeUnit = TimeUnit.MINUTES)
  public void clearCacheScheduled() {
    logger.info("clearCacheScheduled: evicting entries older than 4 hours");
    // every 15 minutes we remove all ScanResult older than 4 hours from the cache
    scanResultCache.evictEntriesOlderThan(Duration.ofHours(4));
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
      Optional<ScanResult> resultFromCache = scanResultCache.find(ip);
      if (resultFromCache.isPresent()) {
        logger.debug("Found matching result in the cache. Now get certificates for {}", hostName);
        TlsProtocolVersion version = TlsProtocolVersion.of(resultFromCache.get().getHighestVersionSupported());
        ProtocolScanResult protocolScanResult = (version != null) ? tlsScanner.scan(version, hostName) : null;
        return CrawlResult.fromCache(visitRequest, resultFromCache.get(), protocolScanResult);
      }
    }
    TlsCrawlResult tlsCrawlResult = scanIfNotBlacklisted(address);
    return CrawlResult.fromScan(visitRequest,  tlsCrawlResult);
  }

  @Transactional
  public void persist(CrawlResult crawlResult) {
    logger.debug("Persisting crawlResult");
    TlsScanResult tlsScanResult = crawlResult.convertToEntity();
    if (crawlResult.isFresh()) {
      scanResultRepository.save(crawlResult.getScanResult());
    }
    saveCertificates(crawlResult.getCertificateChain());
    tlsScanResultRepository.save(tlsScanResult);
  }

  private TlsCrawlResult scanIfNotBlacklisted(InetSocketAddress address) {
    if (blackList.isBlacklisted(address)) {
      return TlsCrawlResult.connectFailed(address, "IP address is blacklisted");
    }
    return tlsScanner.scan(address);
  }

  private void saveCertificates(Optional<List<CertificateInfo>> chain) {
    if (chain.isPresent()) {
      // We have to save the chain in reversed order because of the foreign keys
      List<CertificateInfo> reversed = new ArrayList<>(chain.get());
      Collections.reverse(reversed);
      for (CertificateInfo certificateInfo : reversed) {
        // We always call save, let Hibernate 2nd level cache do its magic
        // Note: this could over-write pre-existing certificates,
        // but we assume the attributes will remain the same
        Certificate certificate = certificateInfo.asEntity();
        certificateRepository.save(certificate);
      }
    }
  }

}
