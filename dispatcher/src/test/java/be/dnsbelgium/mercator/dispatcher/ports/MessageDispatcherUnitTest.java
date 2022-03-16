package be.dnsbelgium.mercator.dispatcher.ports;

import be.dnsbelgium.mercator.common.messaging.dto.DispatcherRequest;
import be.dnsbelgium.mercator.dispatcher.metrics.MetricName;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.core.JmsTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

public class MessageDispatcherUnitTest {

  JmsTemplate jmsTemplate = mock(JmsTemplate.class);

  DispatcherEventRepository repository = mock(DispatcherEventRepository.class);

  private static final Logger logger = getLogger(MessageDispatcherUnitTest.class);

  @Test
  void testDuplicateVisitIds() {
    UUID visitId = UUID.randomUUID();
    DispatcherRequest request1 = new DispatcherRequest(visitId, "abc.be", List.of("coucou", "beuh"));
    DispatcherRequest request2 = new DispatcherRequest(visitId, "xxx.be", List.of("test", "duplicate"));
    DispatcherRequest request3 = new DispatcherRequest(UUID.randomUUID(), "three.be", List.of());

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    List<String> outputQueues = List.of("queue1", "queue2");

    when(repository.save(any(DispatcherEvent.class)))
        .thenReturn(DispatcherEvent.from(request1.getVisitId(), request1))
        .thenThrow(new DataIntegrityViolationException("duplicate key value"))
        .thenReturn(DispatcherEvent.from(request3.getVisitId(), request3));

    MessageDispatcher messageDispatcher = new MessageDispatcher(jmsTemplate, meterRegistry, outputQueues, repository);

    messageDispatcher.receiveAndForward(request1);

    int messagesOutForQueue1 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue1").count();
    int messagesOutForQueue2 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue2").count();
    int duplicateVisitIds = (int) meterRegistry.counter(MetricName.DUPLICATE_VISIT_IDS).count();

    logger.info("messagesOut queue1 = {}", messagesOutForQueue1);
    logger.info("messagesOut queue2 = {}", messagesOutForQueue2);
    logger.info("duplicateVisitIds = {}", duplicateVisitIds);

    assertThat(messagesOutForQueue1).isEqualTo(1);
    assertThat(messagesOutForQueue2).isEqualTo(1);
    assertThat(duplicateVisitIds).isEqualTo(0);

    messageDispatcher.receiveAndForward(request2);

    messagesOutForQueue1 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue1").count();
    messagesOutForQueue2 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue2").count();
    duplicateVisitIds = (int) meterRegistry.counter(MetricName.DUPLICATE_VISIT_IDS).count();

    logger.info("messagesOut queue1 = {}", messagesOutForQueue1);
    logger.info("messagesOut queue2 = {}", messagesOutForQueue2);
    logger.info("duplicateVisitIds = {}", duplicateVisitIds);

    assertThat(messagesOutForQueue1).isEqualTo(1);
    assertThat(messagesOutForQueue2).isEqualTo(1);
    assertThat(duplicateVisitIds).isEqualTo(1);

    messageDispatcher.receiveAndForward(request3);

    messagesOutForQueue1 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue1").count();
    messagesOutForQueue2 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue2").count();

    logger.info("messagesOut queue1 = {}", messagesOutForQueue1);
    logger.info("messagesOut queue2 = {}", messagesOutForQueue2);

    assertThat(messagesOutForQueue1).isEqualTo(2);
    assertThat(messagesOutForQueue2).isEqualTo(2);
  }

  @Test
  void otherException() {
    UUID visitId = UUID.randomUUID();
    DispatcherRequest request1 = new DispatcherRequest(visitId, "abc.be", List.of("coucou", "beuh"));

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    List<String> outputQueues = List.of("queue1", "queue2");

    when(repository.save(any(DispatcherEvent.class)))
        .thenThrow(new DataIntegrityViolationException("Saving failed", new Exception("xyz")));

    MessageDispatcher messageDispatcher = new MessageDispatcher(jmsTemplate, meterRegistry, outputQueues, repository);

    try {
      messageDispatcher.receiveAndForward(request1);
      fail("Should throw Exception");
    } catch (Exception e) {
      logger.info("receiveAndForward threw {}", e.getMessage());
    }

    int messagesOutForQueue1 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue1").count();
    int messagesOutForQueue2 = (int) meterRegistry.counter(MetricName.MESSAGES_OUT, "queue", "queue2").count();
    int duplicateVisitIds = (int) meterRegistry.counter(MetricName.DUPLICATE_VISIT_IDS).count();

    assertThat(messagesOutForQueue1).isEqualTo(0);
    assertThat(messagesOutForQueue2).isEqualTo(0);
    assertThat(duplicateVisitIds).isEqualTo(0);
  }

  @Test
  public void randomUUID() {
    System.out.println("UUID.randomUUID() = " + UUID.randomUUID());
  }

}