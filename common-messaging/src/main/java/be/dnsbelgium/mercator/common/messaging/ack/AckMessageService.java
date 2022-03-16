package be.dnsbelgium.mercator.common.messaging.ack;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

import java.util.UUID;

public class AckMessageService {

  private static final Logger logger = LoggerFactory.getLogger(AckMessageService.class);

  private final JmsTemplate jmsTemplate;
  private final String ackQueueName;
  private final MeterRegistry meterRegistry;

  public AckMessageService(JmsTemplate jmsTemplate, String ackQueueName, MeterRegistry meterRegistry) {
    if (ackQueueName == null || ackQueueName.isEmpty()) {
      throw new IllegalArgumentException("ackQueueName should be set");
    }
    this.jmsTemplate = jmsTemplate;
    this.ackQueueName = ackQueueName;
    this.meterRegistry = meterRegistry;
  }

  public void sendAck(VisitRequest visitRequest, CrawlerModule crawlerModule) {
    var ackCrawlMessage = new AckCrawlMessage(visitRequest.getVisitId(), visitRequest.getDomainName(), crawlerModule);
    jmsTemplate.convertAndSend(ackQueueName, ackCrawlMessage);
    meterRegistry.counter(AckMetric.SENT, "crawlerModule", crawlerModule.name()).increment();
    logger.info("sending ack message from {} for {} ({})", crawlerModule, visitRequest.getDomainName(), visitRequest.getVisitId());
  }

  public void sendAck(UUID visitId, String domainName, CrawlerModule crawlerModule) {
    sendAck(new VisitRequest(visitId, domainName), crawlerModule);
  }
}
