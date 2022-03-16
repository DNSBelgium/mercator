package be.dnsbelgium.mercator.content.ports.async;

import be.dnsbelgium.mercator.content.domain.ContentCrawlService;
import be.dnsbelgium.mercator.content.metrics.MetricName;
import be.dnsbelgium.mercator.content.dto.WappalyzerResolution;
import be.dnsbelgium.mercator.content.dto.wappalyzer.WappalyzerReport;
import be.dnsbelgium.mercator.content.ports.async.model.WappalyzerResponseMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class WappalyzerResolutionListener implements ContentResolutionListener<WappalyzerResponseMessage> {

  private static final Logger logger = LoggerFactory.getLogger(WappalyzerResolutionListener.class);

  private final MeterRegistry meterRegistry;
  private final ContentCrawlService service;

  public WappalyzerResolutionListener(ContentCrawlService service, MeterRegistry meterRegistry) {
    this.service = service;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void contentResolved(WappalyzerResponseMessage response) {
    meterRegistry.counter(MetricName.WAPPALYZER_MESSAGES_IN).increment();
    UUID visitId = response.getRequest().getVisitId();
    if (visitId != null && StringUtils.hasLength(visitId.toString())) {
      logger.info("Storing data for visit {} and domainName = {}", visitId, response.getRequest().getDomainName());
      WappalyzerResolution resolution = toContentResolution(response, visitId);
      service.contentRetrieved(resolution);
    } else {
      logger.warn("WappalyzerResponseMessage has no visitId: {}", response);
  }

}

  @Override
  public WappalyzerResolution toContentResolution(WappalyzerResponseMessage response, UUID visitId) {
    final String error = response.getWappalyzer().getUrls().values().stream()
        .map(WappalyzerReport.WappalyzerUrl::getError)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" | "));
    return new WappalyzerResolution(visitId, response.getRequest().getDomainName(),
                                    response.getRequest().getUrl(), error.isEmpty(), response.getWappalyzer().getUrls(),
                                    response.getWappalyzer().getTechnologies(), error.isEmpty() ? null : error);
  }
}
