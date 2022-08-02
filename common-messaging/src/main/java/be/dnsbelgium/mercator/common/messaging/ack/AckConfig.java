package be.dnsbelgium.mercator.common.messaging.ack;

import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AckConfig {

  public static final String CRAWLER_ACK_QUEUE_NAME = "crawler.ack.queue-name";

  @Bean
  AckMessageService ackMessageService(QueueClient queueClient, MeterRegistry meterRegistry,
                                      @Value("${" + CRAWLER_ACK_QUEUE_NAME + "}") String ackQueueName) {
    return new AckMessageService(queueClient, ackQueueName, meterRegistry);
  }

}

