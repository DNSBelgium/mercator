package be.dnsbelgium.mercator.tls.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.tls.domain.CrawlResult;
import be.dnsbelgium.mercator.tls.domain.FullScanCache;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlerService;
import be.dnsbelgium.mercator.tls.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsCrawler implements Crawler {

  private final MeterRegistry meterRegistry;

  private final TlsCrawlerService crawlerService;

  private final AckMessageService ackMessageService;

  private final FullScanCache fullScanCache;

  private static final Logger logger = getLogger(TlsCrawler.class);

  @Value("${tls.crawler.visit.apex:true}")  private boolean visitApex;
  @Value("${tls.crawler.visit.www:true}")   private boolean visitWww;
  @Value("${tls.crawler.allow.noop:false}") private boolean allowNoop;

  @Autowired
  public TlsCrawler(MeterRegistry meterRegistry, TlsCrawlerService crawlerService, AckMessageService ackMessageService, FullScanCache fullScanCache) {
    this.meterRegistry = meterRegistry;
    this.crawlerService = crawlerService;
    this.ackMessageService = ackMessageService;
    this.fullScanCache = fullScanCache;
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

      if (visitApex) {
        scanHostname("", visitRequest);
      }
      if (visitWww) {
        scanHostname("www.", visitRequest);
      }

      meterRegistry.counter(MetricName.COUNTER_VISITS_COMPLETED).increment();
      ackMessageService.sendAck(visitRequest, CrawlerModule.TLS);

    } finally {
      MDC.remove("domainName");
      MDC.remove("visitId");
    }
  }

  private void scanHostname(String prefix, VisitRequest visitRequest) {
    String hostName = prefix + visitRequest.getDomainName();
    try {
      CrawlResult crawlResult = crawlerService.visit(hostName, visitRequest);
      crawlerService.persist(crawlResult);
      if (crawlResult.isFresh()) {
        fullScanCache.add(Instant.now(), crawlResult.getFullScanEntity());
      }
    } catch (Throwable e) {
      if (exceptionContains(e, "duplicate key value violates unique constraint")) {
        meterRegistry.counter(MetricName.COUNTER_DUPLICATE_VISITS).increment();
        logger.info("crawlResult already in the database hostName={} visitId={} => ignoring this request", hostName, visitRequest.getVisitId());
      } else {
        logAndRethrow(visitRequest, e);
      }
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
