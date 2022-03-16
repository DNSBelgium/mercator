package be.dnsbelgium.mercator.dispatcher;

import be.dnsbelgium.mercator.common.messaging.ack.AckCrawlMessage;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import be.dnsbelgium.mercator.common.messaging.json.DefaultTypeJackson2MessageConverter;
import be.dnsbelgium.mercator.common.messaging.dto.DispatcherRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@Configuration
public class DispatcherConfiguration implements JmsConfig {

  @Bean
  @Override
  public MessageConverter jacksonJmsMessageConverter() {
    return new DefaultTypeJackson2MessageConverter<>(DispatcherRequest.class);
  }

  @Bean
  public DefaultJmsListenerContainerFactory ackJmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                           @Value("${messaging.jms.concurrency}") String concurrency) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setDestinationResolver(new DynamicDestinationResolver());
    factory.setConcurrency(concurrency);
    factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
    factory.setMessageConverter(ackJacksonJmsMessageConverter());

    return factory;
  }

  public MessageConverter ackJacksonJmsMessageConverter() {
    return new DefaultTypeJackson2MessageConverter<>(AckCrawlMessage.class);
  }

}
