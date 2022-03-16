package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.feature.extraction.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class FeatureCrawler implements Crawler {

  private static final Logger logger = getLogger(FeatureCrawler.class);
  private final FeatureService featureService;
  private final MeterRegistry meterRegistry;
  private final boolean updateExistingRows;

  public FeatureCrawler(FeatureService featureService,
                        MeterRegistry meterRegistry,
                        @Value("${feature-extraction.update.existing.rows:false}") boolean updateExistingRows) {
    this.featureService = featureService;
    this.meterRegistry = meterRegistry;
    this.updateExistingRows = updateExistingRows;
    logger.info("updateExistingRows = {}", updateExistingRows);
  }

  @Override
  @JmsListener(destination = "${feature.extraction.input.queue.name}")
  public void process(VisitRequest visitRequest) {
    if (visitRequest == null || visitRequest.getVisitId() == null || visitRequest.getDomainName() == null) {
      logger.info("Received visitRequest without visitId or domain name. visitRequest={} => ignoring", visitRequest);
      meterRegistry.counter(MetricName.COUNTER_VISITS_SKIPPED).increment();
      return;
    }
    setMDC(visitRequest);
    logger.debug("Received VisitRequest for domainName={}", visitRequest.getDomainName());
    try {
      featureService.process(visitRequest, updateExistingRows);
      meterRegistry.counter(MetricName.COUNTER_VISITS_PROCESSED).increment();
      logger.debug("Done extracting features for domainName={}", visitRequest.getDomainName());

      // Later we might have to notify somebody but not for now

    } catch (Exception e) {
      meterRegistry.counter(MetricName.COUNTER_VISITS_FAILED).increment();
      String errorMessage = String.format("failed to extract features for domainName=[%s] because of exception [%s]",
          visitRequest.getDomainName(), e.getMessage());
      logger.error(errorMessage, e);
      // We do re-throw the exception to not acknowledge the message. The message is therefore put back on the queue.
      throw e;
    } finally {
      clearMDC();
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
