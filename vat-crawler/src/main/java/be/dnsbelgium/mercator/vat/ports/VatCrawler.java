package be.dnsbelgium.mercator.vat.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.vat.VatCrawlerService;
import be.dnsbelgium.mercator.vat.domain.DomainNameValidator;
import be.dnsbelgium.mercator.vat.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VatCrawler implements Crawler {

  private final MeterRegistry meterRegistry;
  private final AckMessageService ackMessageService;
  private final VatCrawlerService vatCrawlerService;

  private static final Logger logger = getLogger(VatCrawler.class);

  @Autowired
  public VatCrawler(MeterRegistry meterRegistry, AckMessageService ackMessageService, VatCrawlerService vatCrawlerService) {
    this.meterRegistry = meterRegistry;
    this.ackMessageService = ackMessageService;
    this.vatCrawlerService = vatCrawlerService;
    logger.info("VatCrawler initialized");
  }

  @Override
  @JmsListener(destination = "${vat.crawler.input.queue.name}")
  public void process(VisitRequest visitRequest) {
    if (visitRequest == null || visitRequest.getVisitId() == null || visitRequest.getDomainName() == null) {
      logger.info("Received visitRequest without visitId or domain name. visitRequest={} => ignoring", visitRequest);
      return;
    }
    if (!DomainNameValidator.isValidDomainName(visitRequest.getDomainName())) {
      logger.error("Invalid domain name [{}] included in the visit request. Ignoring this visit request",
          visitRequest.getDomainName());
      return;
    }
    logger.debug("Received VisitRequest for domainName={}", visitRequest.getDomainName());
    String state = "initial";
    try {
      vatCrawlerService.findVatValues(visitRequest);
      state = "after_findVatValues";
      ackMessageService.sendAck(visitRequest, CrawlerModule.VAT);
      state = "after_sendAck";
      logger.debug("findVatValues done for domainName={}", visitRequest.getDomainName());
      meterRegistry.counter(MetricName.COUNTER_SUCCESS_VISITS).increment();
    } catch (Throwable e) {
      meterRegistry.counter(MetricName.COUNTER_FAILED_VISITS).increment();
      logException(visitRequest, e);
      throw e;
    }
  }

  private void logException(VisitRequest visitRequest, Throwable t) {
    // We probably should not adapt our coding to the Logviewer that is being used ...
    // but since Loki treats the log messages line by line context often gets lost when logging exceptions
    // => Log all nested exception messages one-by-one on a single line, all with level ERROR and with context
    // The full stack trace will probably also be logged by our caller (DefaultMessageListenerContainer)
    int depth = 0;
    while (t != null && depth < 20) {
      logger.error("failed to find VAT values for domainName={} and visitId={} Exception-depth={}, exception={} message={}",
          visitRequest.getDomainName(), visitRequest.getVisitId(), depth, t.getClass().getName(), t.getMessage());
      t = t.getCause();
      depth++;
    }
  }
}
