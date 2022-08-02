package be.dnsbelgium.mercator.dispatcher.ports;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.dto.DispatcherRequest;
import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import be.dnsbelgium.mercator.dispatcher.metrics.MetricName;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Component
public class MessageDispatcher {

  private static final Logger LOG = LoggerFactory.getLogger(MessageDispatcher.class);

  private final QueueClient queueClient;
  private final MeterRegistry meterRegistry;
  private final List<String> outputQueues;
  private final DispatcherEventRepository repository;

  public MessageDispatcher(QueueClient queueClient,
                           MeterRegistry meterRegistry,
                           @Value("${dispatcher.queues.forward}") List<String> outputQueues,
                           DispatcherEventRepository repository) {
    this.queueClient = queueClient;
    this.meterRegistry = meterRegistry;
    this.outputQueues = outputQueues;
    this.repository = repository;
    LOG.info("MessageDispatcher: outputQueue: {}", String.join(", ", outputQueues));
  }

  @JmsListener(destination = "${dispatcher.queue.in}")
  @Transactional
  public void receiveAndForward(@Payload(required = false) DispatcherRequest dispatcherRequest) {
    if (dispatcherRequest == null) {
      LOG.debug("received null dispatcherRequest, ignoring");
      return;
    }
    LOG.debug("received {}", dispatcherRequest);
    meterRegistry.counter(MetricName.MESSAGES_IN).increment();

    UUID visitId = dispatcherRequest.getVisitId();
    if (visitId == null) {
      visitId = UUID.randomUUID();
    }

    try {
      VisitRequest visitRequest = new VisitRequest(visitId, dispatcherRequest.getDomainName());
      repository.save(DispatcherEvent.from(visitRequest.getVisitId(), dispatcherRequest));

      for (String outputQueue : outputQueues) {
        queueClient.convertAndSend(outputQueue, visitRequest);
        meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", outputQueue).increment();
      }
    } catch (DataIntegrityViolationException e) {

      if (DispatcherEventRepository.exceptionContains(e, "duplicate key value")) {
        meterRegistry.counter(MetricName.DUPLICATE_VISIT_IDS).increment();
        LOG.warn("Could not save {} because of {}", dispatcherRequest, e.getMessage());
      } else {
        meterRegistry.counter(MetricName.MESSAGES_FAILED).increment();
        throw e;
      }

    }
  }

}
