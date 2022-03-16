package be.dnsbelgium.mercator.content;

import be.dnsbelgium.mercator.common.messaging.ack.AckConfig;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import be.dnsbelgium.mercator.common.messaging.json.DefaultTypeJackson2MessageConverter;
import be.dnsbelgium.mercator.content.ports.async.model.MuppetsResponseMessage;
import be.dnsbelgium.mercator.content.ports.async.model.WappalyzerResponseMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@Configuration
@Import(AckConfig.class)
public class ContentCrawlerConfiguration implements JmsConfig {

  @Override
  public MessageConverter jacksonJmsMessageConverter() {
    return new DefaultTypeJackson2MessageConverter<>(VisitRequest.class);
  }

  @Bean
  public DefaultJmsListenerContainerFactory muppetsJmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              @Value("${messaging.jms.concurrency}") String concurrency) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setDestinationResolver(new DynamicDestinationResolver());
    factory.setConcurrency(concurrency);
    factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
    factory.setMessageConverter(muppetsJacksonJmsMessageConverter());

    return factory;
  }

  public MessageConverter muppetsJacksonJmsMessageConverter() {
    return new DefaultTypeJackson2MessageConverter<>(MuppetsResponseMessage.class);
  }

  @Bean
  public DefaultJmsListenerContainerFactory wappalyzerJmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              @Value("${messaging.jms.concurrency}") String concurrency) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setDestinationResolver(new DynamicDestinationResolver());
    factory.setConcurrency(concurrency);
    factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
    factory.setMessageConverter(wappalyzerJacksonJmsMessageConverter());

    return factory;
  }

  public MessageConverter wappalyzerJacksonJmsMessageConverter() {
    return new DefaultTypeJackson2MessageConverter<>(WappalyzerResponseMessage.class);
  }

}

