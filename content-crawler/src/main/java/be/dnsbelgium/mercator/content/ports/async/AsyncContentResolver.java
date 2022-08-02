package be.dnsbelgium.mercator.content.ports.async;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import be.dnsbelgium.mercator.content.config.ResolvingConfigurationProperties;
import be.dnsbelgium.mercator.content.domain.content.ContentResolver;
import be.dnsbelgium.mercator.content.metrics.MetricName;
import be.dnsbelgium.mercator.content.ports.async.model.RequestMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

@Component
public class AsyncContentResolver implements ContentResolver {

  private static final Logger logger = LoggerFactory.getLogger(AsyncContentResolver.class);
  private static final String PREFIX = "be.dnsbelgium.mercator.content.ports.async.model.";

  private final MeterRegistry meterRegistry;
  private final ResolvingConfigurationProperties resolvingConfigurationProperties;
  private final QueueClient queueClient;

  public AsyncContentResolver(MeterRegistry meterRegistry,
                              ResolvingConfigurationProperties resolvingConfigurationProperties,
                              QueueClient queueClient) {
    this.meterRegistry = meterRegistry;
    this.resolvingConfigurationProperties = resolvingConfigurationProperties;
    this.queueClient = queueClient;
  }

  public void requestContentResolving(VisitRequest visitRequest, List<String> urlCandidates) {
    for (String url : urlCandidates) {
      for (Map.Entry<String, String> queueName : resolvingConfigurationProperties.getRequestQueues().entrySet()) {
        final String key = queueName.getKey();
        final String className = key.substring(0, 1).toUpperCase() + key.substring(1);
        try {
          Class<?> clazz = Class.forName(PREFIX + className + "RequestMessage");
          RequestMessage request = (RequestMessage) clazz.getDeclaredConstructor().newInstance();
          request.setVisitId(visitRequest.getVisitId());
          request.setDomainName(visitRequest.getDomainName());
          request.setUrl(url);
          logger.info("Putting message {} on queue {}", request, queueName.getKey());
          queueClient.convertAndSend(queueName.getValue(), request);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
          logger.error("{} is not a correct argument, cannot find the associated RequestMessage class", key, e);
        }
      }
      meterRegistry.counter(MetricName.MESSAGES_OUT).increment();
    }
  }

}
