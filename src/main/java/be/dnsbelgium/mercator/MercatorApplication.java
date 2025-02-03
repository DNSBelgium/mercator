package be.dnsbelgium.mercator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"be.dnsbelgium.mercator"} ,
exclude = {
        DataSourceAutoConfiguration.class,
        BatchAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
})
@EnableBatchProcessing
public class MercatorApplication {

  @Value("${jms.concurrency:10}")
  private String jmsConcurrency;

  private static final Logger logger = LoggerFactory.getLogger(MercatorApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(MercatorApplication.class, args);
  }


//  @Bean
//  @ConditionalOnProperty(value = "use.sqs", havingValue = "false")
//  public Scheduler scheduler(DuckDataSource dataSource, WorkQueue workQueue, Repository repository) {
//    return new Scheduler(dataSource, workQueue, repository);
//  }


//  @Bean // Serialize message content to json using TextMessage
//  //@ConditionalOnProperty(value = "use.sqs", havingValue = "false")
//  public MessageConverter jacksonJmsMessageConverter() {
//    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//    converter.setTargetType(MessageType.TEXT);
//    converter.setTypeIdPropertyName("_type");
//    return converter;
//  }

//  @Bean
//  public JmsListenerContainerFactory<?> myFactory(
//          @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory,
//          DefaultJmsListenerContainerFactoryConfigurer configurer) {
//    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//    // This provides all auto-configured defaults to this factory, including the message converter
//    configurer.configure(factory, connectionFactory);
//    // You could still override some settings if necessary.
//    logger.info("jmsConcurrency: {}", jmsConcurrency);
//    factory.setConcurrency(jmsConcurrency);
//    return factory;
//  }

}
