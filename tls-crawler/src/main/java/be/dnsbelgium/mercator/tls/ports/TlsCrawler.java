package be.dnsbelgium.mercator.tls.ports;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.tls.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsCrawler implements Crawler {

  private final MeterRegistry meterRegistry;

  private static final Logger logger = getLogger(TlsCrawler.class);

  @Autowired
  public TlsCrawler(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  @JmsListener(destination = "${tls.crawler.input.queue.name}")
  public void process(VisitRequest visitRequest) {
    if (visitRequest == null || visitRequest.getVisitId() == null || visitRequest.getDomainName() == null) {
      logger.info("Received visitRequest without visitId or domain name. visitRequest={} => ignoring", visitRequest);
      return;
    }
    // Should we validate the domain name ?
    logger.debug("Received VisitRequest for domainName={}", visitRequest.getDomainName());
    try {

      logger.info("processing {}", visitRequest);
      meterRegistry.counter(MetricName.COUNTER_VISITS_COMPLETED).increment();

    } catch (Throwable e) {
      meterRegistry.counter(MetricName.COUNTER_VISITS_FAILED).increment();
      logException(visitRequest, e);
      throw e;
    }
  }

  public void logException(VisitRequest visitRequest, Throwable t) {
    logger.error("Failed to check TLS support for domainName={} and visitId={} exception={} message={}",
        visitRequest.getDomainName(), visitRequest.getVisitId(), t.getClass().getName(), t.getMessage());
  }

}
