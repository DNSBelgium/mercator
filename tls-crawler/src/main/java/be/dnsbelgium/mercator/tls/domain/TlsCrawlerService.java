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
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
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

  // a cache of certificates that have already been saved to the database
  private final Set<String> savedCertificates = new HashSet<>();

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
    // TODO: pre-populate savedCertificates with certificates in DB ?
    // TODO: pre-populate cache of TlsScanResult's ??
  }

  @Scheduled(fixedRate = 15, initialDelay = 15, timeUnit = TimeUnit.MINUTES)
  public void clearCacheScheduled() {
    logger.info("clearCacheScheduled: evicting entries older than 4 hours");
    // every 15 minutes we remove all ScanResult older than 4 hours from the cache
    scanResultCache.evictEntriesOlderThan(Duration.ofHours(4));
  }

  @Transactional
  public TlsScanResult crawl(VisitRequest visitRequest) {
    // TODO: better to start @Transactional method only after the actual crawling
    logger.info("Crawling {}", visitRequest);
    return doCrawl(visitRequest);
  }

  private TlsScanResult doCrawl(VisitRequest visitRequest) {
    ZonedDateTime crawlTimestamp = ZonedDateTime.now();
    String hostName = visitRequest.getDomainName();
    InetSocketAddress address = new InetSocketAddress(hostName, destinationPort);

    if (!address.isUnresolved()) {
      String ip = address.getAddress().getHostAddress();
      Optional<ScanResult> resultFromCache = scanResultCache.find(ip);
      if (resultFromCache.isPresent()) {
        logger.info("Found matching result in the cache");
        ScanResult scanResult = resultFromCache.get();
        return fromCache(visitRequest, hostName, scanResult);
      }
    }
    TlsCrawlResult crawlResult = scanIfNotBlacklisted(address);
    ScanResult scanResult = convert(crawlTimestamp, crawlResult);
    //scanResultCache.add(Instant.now(), scanResult);

    TlsScanResult tlsScanResult = TlsScanResult.builder()
        .scanResult(scanResult)
        .visitId(visitRequest.getVisitId())
        .domainName(visitRequest.getDomainName())
        .hostName(visitRequest.getDomainName())
        .crawlTimestamp(crawlTimestamp)
        .hostNameMatchesCertificate(crawlResult.isHostNameMatchesCertificate())
        .build();

    saveCertificates(crawlResult);
    scanResultRepository.save(scanResult);
    return tlsScanResultRepository.save(tlsScanResult);
  }

  private TlsCrawlResult scanIfNotBlacklisted(InetSocketAddress address) {
    if (blackList.isBlacklisted(address)) {
      return TlsCrawlResult.connectFailed(address, "IP address is blacklisted");
    }
    return tlsScanner.scan(address);
  }

  private TlsScanResult fromCache(VisitRequest visitRequest, String hostName, ScanResult resultFromCache) {
    // When checking TLS support, we use a HostnameVerifier to set isHostNameMatchesCertificate
    // Here we should check again if the hostName matches the received cert chain
    // because hostName from VisitRequest probably differs from resultFromCache.get().getServerName()
    // BUT we do not have an SSLSession ...
    // so we just check the SubjectAltNames of the certificate from the cache

    boolean match = false;
    Certificate leafCertificate = resultFromCache.getLeafCertificate();
    if (leafCertificate != null) {
      List<String> subjectAltNames = leafCertificate.getSubjectAltNames();
      if (subjectAltNames.contains(hostName)) {
        logger.debug("{} found in the getSubjectAltNames of the cached ScanResult for {}", hostName, resultFromCache.getServerName());
        match = true;
      }
    }
    if (!match) {
      logger.info("hostName {} does NOT match with subjectAltNames of the cached ScanResult for {}", hostName, resultFromCache.getServerName());
    }
    TlsScanResult tlsScanResult = TlsScanResult.builder()
        .scanResult(resultFromCache)
        .visitId(visitRequest.getVisitId())
        .domainName(visitRequest.getDomainName())
        .hostName(visitRequest.getDomainName())
        .crawlTimestamp(ZonedDateTime.now())
        .hostNameMatchesCertificate(match)
        .build();
    return tlsScanResultRepository.save(tlsScanResult);
  }

  private void saveCertificates(TlsCrawlResult tlsCrawlResult) {
    Optional<List<CertificateInfo>> chain = tlsCrawlResult.getCertificateChain();
    if (chain.isPresent()) {
      // We have to save the chain in reversed order because of the foreign keys
      List<CertificateInfo> reversed = new ArrayList<>(chain.get());
      Collections.reverse(reversed);
      for (CertificateInfo certificateInfo : reversed) {
        save(certificateInfo);
      }
    }
  }

  private void save(CertificateInfo certificateInfo) {
    String fingerprint = certificateInfo.getSha256Fingerprint();
    if (!savedCertificates.contains(fingerprint)) {
      Certificate certificate = asEntity(certificateInfo);
      logger.info("certificate = {}", certificate.getSha256fingerprint());
      // This could over-write pre-existing certificates. Do we care?
      certificateRepository.save(certificate);
      savedCertificates.add(fingerprint);
      logger.info("We saved certificate with fingerprint {}", fingerprint);
    } else {
      logger.debug("Certificate with fingerprint {} already in the database. DN = {}", fingerprint, MDC.get("domainName"));
    }
  }

  public Certificate asEntity(CertificateInfo certificateInfo) {
    String signedBy = (certificateInfo.getSignedBy() == null) ?
        null : certificateInfo.getSignedBy().getSha256Fingerprint();
    return Certificate.builder()
        .sha256fingerprint(certificateInfo.getSha256Fingerprint())
        .version(certificateInfo.getVersion())
        .subjectAltNames(certificateInfo.getSubjectAlternativeNames())
        .serialNumber(certificateInfo.getSerialNumber().toString())
        .signatureHashAlgorithm(certificateInfo.getSignatureHashAlgorithm())
        .notBefore(certificateInfo.getNotBefore())
        .notAfter(certificateInfo.getNotAfter())
        .publicKeyLength(certificateInfo.getPublicKeyLength())
        .publicKeySchema(certificateInfo.getPublicKeySchema())
        .issuer(certificateInfo.getIssuer())
        .subject(certificateInfo.getSubject())
        .signedBySha256(signedBy)
        .build();
  }

  public ScanResult convert(ZonedDateTime timestamp,  TlsCrawlResult tlsCrawlResult) {
    var tls13 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_3);
    var tls12 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_2);
    var tls11 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_1);
    var tls10 = tlsCrawlResult.get(TlsProtocolVersion.TLS_1_0);
    var ssl3 = tlsCrawlResult.get(TlsProtocolVersion.SSL_3);
    var ssl2 = tlsCrawlResult.get(TlsProtocolVersion.SSL_2);

    Optional<CertificateInfo> peerCertificate = tlsCrawlResult.getPeerCertificate();
    Certificate leafCertificate = peerCertificate.map(this::asEntity).orElse(null);

    boolean certificateExpired = peerCertificate.isPresent()
        && Instant.now().isAfter(peerCertificate.get().getNotAfter());

    boolean certificateTooSoon = peerCertificate.isPresent()
        && Instant.now().isBefore(peerCertificate.get().getNotBefore());

    String lowestVersion  = tlsCrawlResult.getLowestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);
    String highestVersion = tlsCrawlResult.getHighestVersionSupported().map(TlsProtocolVersion::getName).orElse(null);

    return ScanResult.builder()
        .lowestVersionSupported(lowestVersion)
        .highestVersionSupported(highestVersion)
        .leafCertificate(leafCertificate)
        .supportTls_1_3(tls13.isHandshakeOK())
        .supportTls_1_2(tls12.isHandshakeOK())
        .supportTls_1_1(tls11.isHandshakeOK())
        .supportTls_1_0(tls10.isHandshakeOK())
        .supportSsl_3_0(ssl3.isHandshakeOK())
        .supportSsl_2_0(ssl2.isHandshakeOK())
        .errorTls_1_3(tls13.getErrorMessage())
        .errorTls_1_2(tls12.getErrorMessage())
        .errorTls_1_1(tls11.getErrorMessage())
        .errorTls_1_0(tls10.getErrorMessage())
        .errorSsl_3_0(ssl3.getErrorMessage())
        .errorSsl_2_0(ssl2.getErrorMessage())
        .millis_tls_1_0(tls10.getScanDuration().toMillis())
        .millis_tls_1_1(tls11.getScanDuration().toMillis())
        .millis_tls_1_2(tls12.getScanDuration().toMillis())
        .millis_tls_1_3(tls13.getScanDuration().toMillis())
        .millis_ssl_3_0(ssl3.getScanDuration().toMillis())
        .millis_ssl_2_0(ssl2.getScanDuration().toMillis())
        .ip(tls13.getIpAddress())
        .connectOk(tls13.isConnectOK())
        .serverName(tls13.getServerName())
        .selectedCipherTls_1_3(tls13.getSelectedCipherSuite())
        .selectedCipherTls_1_2(tls12.getSelectedCipherSuite())
        .selectedCipherTls_1_1(tls11.getSelectedCipherSuite())
        .selectedCipherTls_1_0(tls10.getSelectedCipherSuite())
        .selectedCipherSsl_3_0(ssl3.getSelectedCipherSuite())
        .crawlTimestamp(timestamp)
        .certificateExpired(certificateExpired)
        .certificateTooSoon(certificateTooSoon)
        .chainTrustedByJavaPlatform(tlsCrawlResult.isChainTrustedByJavaPlatform())
        .build();
  }

}
