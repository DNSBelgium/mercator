package be.dnsbelgium.mercator.common.messaging.jms;

import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import be.dnsbelgium.mercator.common.messaging.queue.QueueMessage;
import org.springframework.jms.core.JmsTemplate;

public class JmsQueueClient implements QueueClient {

  final JmsTemplate jmsTemplate;

  public JmsQueueClient(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  @Override
  public void convertAndSend(String outputQueue, QueueMessage queueMessage) {
    jmsTemplate.convertAndSend(outputQueue, queueMessage);
  }

}
