package be.dnsbelgium.mercator.common.messaging.ack;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class AckConfig {

  public static final String CRAWLER_ACK_QUEUE_NAME = "crawler.ack.queue-name";

  @Bean
  AckMessageService ackMessageService(JmsTemplate jmsTemplate, MeterRegistry meterRegistry,
                                      @Value("${" + CRAWLER_ACK_QUEUE_NAME + "}") String ackQueueName) {
    return new AckMessageService(jmsTemplate, ackQueueName, meterRegistry);
  }

}

