package be.dnsbelgium.mercator.dispatcher.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckCrawlMessage;
import be.dnsbelgium.mercator.common.messaging.ack.AckMetric;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Optional;

@Component
public class AcknowledgeListener {

  private static final Logger logger = LoggerFactory.getLogger(AcknowledgeListener.class);

  private final DispatcherEventRepository repository;
  private final QueueClient queueClient;
  private final String outputQueue;
  private final MeterRegistry meterRegistry;

  private final long EXPECTED_ACKS = CrawlerModule.numberOfEnabledModules();

  public AcknowledgeListener(DispatcherEventRepository repository, QueueClient queueClient,
                             @Value("${dispatcher.queue.out}") String outputQueue, MeterRegistry meterRegistry) {
    this.repository = repository;
    this.queueClient = queueClient;
    this.outputQueue = outputQueue;
    this.meterRegistry = meterRegistry;
  }

  @JmsListener(destination = "${dispatcher.queue.ack}", containerFactory = "ackJmsListenerContainerFactory")
  @Transactional
  public void ack(AckCrawlMessage ackCrawlMessage) {
    logger.debug("Ack received for visitId [{}] from {}", ackCrawlMessage.getVisitId(), ackCrawlMessage.getCrawlerModule());
    Optional<DispatcherEvent> dispatcherEventOptional = repository.findById(ackCrawlMessage.getVisitId());
    if (dispatcherEventOptional.isEmpty()) {
      logger.error("Received a visit id [{}] that is not present in the DB", ackCrawlMessage.getVisitId());
      return; // Ignore message
    }
    DispatcherEvent dispatcherEvent = dispatcherEventOptional.get();
    dispatcherEvent.ack(ackCrawlMessage.getCrawlerModule());
    repository.save(dispatcherEvent);
    meterRegistry.counter(AckMetric.READ, "crawlerModule", ackCrawlMessage.getCrawlerModule().name()).increment();

    int ackCount = dispatcherEvent.getAcks().size();
    logger.debug("We have {} acks for {}", ackCount, ackCrawlMessage.getVisitId());

    if (ackCount == EXPECTED_ACKS) {
      // All ack messages received. Sending output signal.
      logger.info("All ack messages received for {}. Sending out a signal.", ackCrawlMessage.getVisitId());
      queueClient.convertAndSend(outputQueue, new VisitRequest(ackCrawlMessage.getVisitId(), ackCrawlMessage.getDomainName()));
      meterRegistry.counter(AckMetric.ALL_MODULES_COMPLETE).increment();
    }
  }

}
