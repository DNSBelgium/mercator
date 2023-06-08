package be.dnsbelgium.mercator.smtp.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.smtp.SmtpCrawlService;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConversationCache;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpVisit;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class SmtpCrawler implements Crawler {

  private static final Logger logger = getLogger(SmtpCrawler.class);

  private final MeterRegistry meterRegistry;
  private final SmtpCrawlService crawlService;
  private final AtomicInteger concurrentVisits = new AtomicInteger();
  private final AckMessageService ackMessageService;
  private final SmtpConversationCache cache;
  @Autowired
  public SmtpCrawler(MeterRegistry meterRegistry, SmtpCrawlService crawlService, AckMessageService ackMessageService
    , SmtpConversationCache cache) {
    this.meterRegistry = meterRegistry;
    this.crawlService = crawlService;
    this.ackMessageService = ackMessageService;
    this.cache = cache;
  }

  @Override
  @JmsListener(destination = "${smtp.crawler.input.queue.name}")
  public void process(VisitRequest visitRequest) throws Exception {
    if (visitRequest == null || visitRequest.getVisitId() == null || visitRequest.getDomainName() == null) {
      logger.info("Received visitRequest without visitId or domain name. visitRequest={} => ignoring", visitRequest);
      return;
    }
    setMDC(visitRequest);
    logger.debug("Received VisitRequest for domainName={}", visitRequest.getDomainName());
    Optional<SmtpVisitEntity> existingResult = crawlService.find(visitRequest.getVisitId());
    logger.info("SmtpCrawler.process (after find) : tx active: {}", TransactionSynchronizationManager.isActualTransactionActive());
    if (existingResult.isPresent()) {
      logger.info("visit {} already exists => skipping", visitRequest);
      return;
    }

    meterRegistry.gauge(MetricName.GAUGE_CONCURRENT_VISITS, concurrentVisits.incrementAndGet());
    try {
      SmtpVisit smtpVisit = crawlService.retrieveSmtpInfo(visitRequest);
      logger.info("SmtpCrawler.process (after retrieveSmtpInfo): tx active: {}", TransactionSynchronizationManager.isActualTransactionActive());
      saveAndIgnoreDuplicate(visitRequest, smtpVisit);
      ackMessageService.sendAck(visitRequest, CrawlerModule.SMTP);
      logger.info("retrieveSmtpInfo done for domainName={}", visitRequest.getDomainName());
    } catch (Exception e) {
      meterRegistry.counter(MetricName.COUNTER_FAILED_VISITS).increment();
      String errorMessage = String.format("failed to analyze SMTP for domainName=[%s] because of exception [%s]",
        visitRequest.getDomainName(), e.getMessage());
      logger.error(errorMessage, e);
      // We do re-throw the exception to not acknowledge the message. The message is therefore put back on the queue.
      // Since June 2020, we enable DLQ on SQS, allowing us to not care or to not keep a state/counter/ratelimiter
      // to ignore messages that are reprocessed more than x times.
      throw e;
    } finally {
      meterRegistry.gauge(MetricName.GAUGE_CONCURRENT_VISITS, concurrentVisits.decrementAndGet());
      clearMDC();
    }
  }

  private void saveAndIgnoreDuplicate(VisitRequest visitRequest, SmtpVisit smtpVisit) throws UnexpectedRollbackException {
    logger.info("SmtpCrawler.saveAndIgnoreDuplicate : tx active: {}", TransactionSynchronizationManager.isActualTransactionActive());
    try {
      List<SmtpConversation> newConversations = crawlService.save(smtpVisit);
      logger.info("SmtpCrawler.saveAndIgnoreDuplicate after save : tx active: {}", TransactionSynchronizationManager.isActualTransactionActive());
      for (SmtpConversation conversation : newConversations){
        cache.add(conversation.getIp(), conversation);
      }
    } catch (UnexpectedRollbackException e) {
      logger.info("UnexpectedRollbackException: {}", e.getMessage());
      Optional<SmtpVisitEntity> existingResult = crawlService.find(visitRequest.getVisitId());
      if (existingResult.isPresent()) {
        logger.info("Save failed because SmtpCrawlResult already existed");
        // swallow exception so that message will be removed from SQS queue (and ack will be sent)
      } else {
        logger.error("True UnexpectedRollbackException");
        throw e;
      }
    }
  }

  private void setMDC(VisitRequest visitRequest) {
    MDC.put("domainName", visitRequest.getDomainName());
    MDC.put("visitId", visitRequest.getVisitId().toString());
  }

  private void clearMDC() {
    MDC.remove("domainName");
    MDC.remove("visitId");
  }

}
