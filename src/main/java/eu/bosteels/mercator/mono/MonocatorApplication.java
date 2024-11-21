package eu.bosteels.mercator.mono;

import be.dnsbelgium.mercator.DuckDataSource;
import eu.bosteels.mercator.mono.persistence.Repository;
import eu.bosteels.mercator.mono.scheduling.Scheduler;
import eu.bosteels.mercator.mono.scheduling.WorkQueue;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"be.dnsbelgium.mercator", "eu.bosteels.mercator"} ,
exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
})
public class MonocatorApplication {

  @Value("${jms.concurrency:10}")
  private String jmsConcurrency;

  private static final Logger logger = LoggerFactory.getLogger(MonocatorApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(MonocatorApplication.class, args);
  }

//  @Bean
//  @ConditionalOnProperty(value = "use.sqs", havingValue = "false")
//  public Scheduler scheduler(DuckDataSource dataSource, JmsTemplate jmsTemplate, Repository repository) {
//    return new Scheduler(dataSource, jmsTemplate, repository);
//  }

  @Bean
  @ConditionalOnProperty(value = "use.sqs", havingValue = "false")
  public Scheduler scheduler(DuckDataSource dataSource, WorkQueue workQueue, Repository repository) {
    return new Scheduler(dataSource, workQueue, repository);
  }


  @Bean // Serialize message content to json using TextMessage
  //@ConditionalOnProperty(value = "use.sqs", havingValue = "false")
  public MessageConverter jacksonJmsMessageConverter() {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTargetType(MessageType.TEXT);
    converter.setTypeIdPropertyName("_type");
    return converter;
  }

  @Bean
  public JmsListenerContainerFactory<?> myFactory(
          @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory,
          DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    // This provides all auto-configured defaults to this factory, including the message converter
    configurer.configure(factory, connectionFactory);
    // You could still override some settings if necessary.
    logger.info("jmsConcurrency: {}", jmsConcurrency);
    factory.setConcurrency(jmsConcurrency);
    return factory;
  }

}
