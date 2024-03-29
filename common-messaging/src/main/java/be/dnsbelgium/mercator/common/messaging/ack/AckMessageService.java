package be.dnsbelgium.mercator.common.messaging.ack;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AckMessageService {

  private static final Logger logger = LoggerFactory.getLogger(AckMessageService.class);

  private final QueueClient queueClient;
  private final String ackQueueName;
  private final MeterRegistry meterRegistry;

  public AckMessageService(QueueClient queueClient, String ackQueueName, MeterRegistry meterRegistry) {
    if (ackQueueName == null || ackQueueName.isEmpty()) {
      throw new IllegalArgumentException("ackQueueName should be set");
    }
    this.queueClient = queueClient;
    this.ackQueueName = ackQueueName;
    this.meterRegistry = meterRegistry;
  }

  public void sendAck(VisitRequest visitRequest, CrawlerModule crawlerModule) {
    var ackCrawlMessage = new AckCrawlMessage(visitRequest.getVisitId(), visitRequest.getDomainName(), crawlerModule);
    queueClient.convertAndSend(ackQueueName, ackCrawlMessage);
    meterRegistry.counter(AckMetric.SENT, "crawlerModule", crawlerModule.name()).increment();
    logger.info("sending ack message from {} for {} ({})", crawlerModule, visitRequest.getDomainName(), visitRequest.getVisitId());
  }

  public void sendAck(UUID visitId, String domainName, CrawlerModule crawlerModule) {
    sendAck(new VisitRequest(visitId, domainName), crawlerModule);
  }
}
