package be.dnsbelgium.mercator.content.ports.async;

import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import be.dnsbelgium.mercator.content.config.ResolvingConfigurationProperties;
import be.dnsbelgium.mercator.content.domain.ContentCrawlService;
import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import be.dnsbelgium.mercator.content.metrics.MetricName;
import be.dnsbelgium.mercator.content.ports.async.model.MuppetsRequestMessage;
import be.dnsbelgium.mercator.content.ports.async.model.MuppetsResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class MuppetsResolutionListener implements ContentResolutionListener<MuppetsResponseMessage> {

  private static final Logger logger = LoggerFactory.getLogger(MuppetsResolutionListener.class);

  private final MeterRegistry meterRegistry;
  private final ContentCrawlService service;
  private final ObjectMapper objectMapper;

  private final ResolvingConfigurationProperties configuration;

  private final QueueClient queueClient;

  private static final int MAX_RETRIES = 1;

  public MuppetsResolutionListener(ContentCrawlService service, ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry, ResolvingConfigurationProperties configuration, QueueClient queueClient) {
    this.service = service;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.configuration = configuration;
    this.queueClient = queueClient;
  }

  @Override
  public void contentResolved(MuppetsResponseMessage response) throws JsonProcessingException {
    meterRegistry.counter(MetricName.MUPPETS_MESSAGES_IN).increment();
    MuppetsRequestMessage request = response.getRequest();
    UUID visitId = request.getVisitId();

    if (visitId != null && StringUtils.hasLength(visitId.toString())) {
      if (!response.getErrors().isEmpty() && request.getRetries() < MAX_RETRIES) {
        performRetry(request);
      } else {
        logger.info("Storing data for visit {} and domainName = {}", visitId, response.getRequest().getDomainName());
        MuppetsResolution resolution = toContentResolution(response, visitId);
        service.contentRetrieved(resolution);
      }
    } else {
      logger.warn("MuppetsResponseMessage has no visitId: {}", response);
    }
  }

  private void performRetry(MuppetsRequestMessage request) {
    UUID visitId = request.getVisitId();
    logger.info("Retrying visit {} and domainName = {} (retry #{})", visitId, request.getDomainName(), request.getRetries());

    request.setRetries(request.getRetries() + 1);
    String queueName = this.configuration.getRequestQueues().get("muppets");
    if (queueName == null) {
      logger.error("Could not put visit {} back in queue, because no queue was given", visitId);
      return;
    }

    this.queueClient.convertAndSend(queueName, request);
  }

  @Override
  public MuppetsResolution toContentResolution(MuppetsResponseMessage response, UUID visitId) throws JsonProcessingException {
    MuppetsResolution resolution;
    if (response.getErrors().isEmpty()) {
      resolution = new MuppetsResolution(visitId, response.getRequest().getDomainName(), response.getRequest().getUrl(),
                                         true, null, response.getUrl(), response.getBucket(),
                                         response.getScreenshotFile(), response.getHtmlFile(), response.getHtmlLength(),
                                         response.getHarFile(), objectMapper.writeValueAsString(response.getMetrics()),
                                         response.getIpv4(), response.getIpv6(), response.getBrowserVersion(), response.getRequest().getRetries(), response.isHtmlSkipped(), response.isScreenshotSkipped(), response.isHarSkipped());
    } else {
      resolution = new MuppetsResolution(visitId, response.getRequest().getDomainName(), response.getRequest().getUrl(), false,
                                         objectMapper.writeValueAsString(response.getErrors()), response.getUrl(),
                                         null, null, null, null, null, null,
                                         response.getIpv4(), response.getIpv6(), response.getBrowserVersion(), response.getRequest().getRetries(), response.isHtmlSkipped(), response.isScreenshotSkipped(), response.isHarSkipped());
    }

    return resolution;
  }

}
