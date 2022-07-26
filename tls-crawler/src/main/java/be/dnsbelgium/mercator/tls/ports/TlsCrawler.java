package be.dnsbelgium.mercator.tls.ports;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.tls.domain.CrawlResult;
import be.dnsbelgium.mercator.tls.domain.ScanResultCache;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlerService;
import be.dnsbelgium.mercator.tls.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsCrawler implements Crawler {

  private final MeterRegistry meterRegistry;

  private final TlsCrawlerService crawlerService;

  private final ScanResultCache scanResultCache;

  private static final Logger logger = getLogger(TlsCrawler.class);

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  public TlsCrawler(MeterRegistry meterRegistry, TlsCrawlerService crawlerService, ScanResultCache scanResultCache) {
    this.meterRegistry = meterRegistry;
    this.crawlerService = crawlerService;
    this.scanResultCache = scanResultCache;
  }

  @Override
  @JmsListener(destination = "${tls.crawler.input.queue.name}")
  public void process(VisitRequest visitRequest) {
    if (visitRequest == null || visitRequest.getVisitId() == null || visitRequest.getDomainName() == null) {
      logger.info("Received visitRequest without visitId or domain name. visitRequest={} => ignoring", visitRequest);
      return;
    }
    try {
      MDC.put("domainName", visitRequest.getDomainName());
      MDC.put("visitId", visitRequest.getVisitId().toString());
      logger.debug("Received VisitRequest for domainName={}", visitRequest.getDomainName());

      CrawlResult crawlResult = crawlerService.visit(visitRequest);
      crawlerService.persist(crawlResult);
      meterRegistry.counter(MetricName.COUNTER_VISITS_COMPLETED).increment();

      if (crawlResult.isFresh()) {
        scanResultCache.add(Instant.now(), crawlResult.getScanResult());
      }

    } catch (DataIntegrityViolationException e) {
      logger.info("DataIntegrityViolationException: {}", e.getMessage());
      if (exceptionContains(e, "duplicate key value violates unique constraint")) {
        meterRegistry.counter(MetricName.COUNTER_DUPLICATE_VISITS).increment();
        logger.info("visit_id already in the database: {} => ignoring this request", visitRequest.getVisitId());
      } else {
        logAndRethrow(visitRequest, e);
      }

    } catch (Throwable e) {
      logAndRethrow(visitRequest, e);
    } finally {
      MDC.remove("domainName");
      MDC.remove("visitId");
    }
  }

  @SneakyThrows
  private void logAndRethrow(VisitRequest visitRequest, Throwable e) {
    meterRegistry.counter(MetricName.COUNTER_VISITS_FAILED).increment();
    logger.error("Failed to check TLS support for domainName={} and visitId={} exception={} message={}",
        visitRequest.getDomainName(), visitRequest.getVisitId(), e.getClass().getName(), e.getMessage());
    throw e;
  }

  @SuppressWarnings("SameParameterValue")
  private boolean exceptionContains(Throwable throwable, String message) {
    while (throwable != null) {
      if (StringUtils.contains(throwable.getMessage(), message)) {
        return true;
      }
      if (throwable.getCause() == throwable) {
        return false;
      }
      throwable = throwable.getCause();
    }
    return false;
  }

}
