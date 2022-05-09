package be.dnsbelgium.mercator.dns.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlService;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class DnsCrawler implements Crawler {

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawler.class);

  private final DnsCrawlService service;
  private final AckMessageService ackMessageService;
  private final MeterRegistry meterRegistry;

  public DnsCrawler(DnsCrawlService service, AckMessageService ackMessageService, MeterRegistry meterRegistry) {
    this.service = service;
    this.ackMessageService = ackMessageService;
    this.meterRegistry = meterRegistry;
  }

  @JmsListener(destination = "${dns.crawler.input.queue.name}")
  public void process(VisitRequest visitRequest) throws Exception {
    logger.debug("Received VisitRequest for domainName={}", visitRequest.getDomainName());
    if (visitRequest.getVisitId() == null) {
      logger.warn("Cannot process a request without visitId => skipping");
      return;
    }
    try {
      meterRegistry.timer(MetricName.RESOLVE_ALL).record(() -> service.retrieveDnsRecords(visitRequest));
      ackMessageService.sendAck(visitRequest, CrawlerModule.DNS);
      logger.info("retrieveDnsRecords done for domainName={}", visitRequest.getDomainName());
    } catch (Exception e) {
      logger.error("failed to retrieveDnsRecords for domainName={} because of {}", visitRequest.getDomainName(), e.getMessage(), e);

      // We do re-throw the exception to not acknowledge the message. The message is therefore put back on the queue.
      // Since July 2020, we enable DLQ on SQS, allowing us to not care or to not keep a state/counter/ratelimiter to ignore messages that are reprocessed more than x times.
      throw e;
    }
  }

}
