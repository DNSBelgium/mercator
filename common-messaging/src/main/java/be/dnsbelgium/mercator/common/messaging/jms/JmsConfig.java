package be.dnsbelgium.mercator.common.messaging.jms;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.json.DefaultTypeJackson2MessageConverter;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableJms
public interface JmsConfig {

  Logger logger = getLogger(JmsConfig.class);

  @Bean
  default AmazonSQSClientBuilder amazonSQSClientBuilder(ClientConfiguration clientConfiguration,
                                                        @Value("${localstack.url:}") String localstackUrl,
                                                        @Value("${cloud.aws.credentials.accessKey:accesskey}") String accessKey,
                                                        @Value("${cloud.aws.credentials.secretKey:secretkey}") String secretKey,
                                                        @Value("${cloud.aws.region.static:eu-west-1}") String region) {
    logger.info("localstackUrl={} region={}", localstackUrl, region);
    if (localstackUrl.isEmpty()) {
      return AmazonSQSClientBuilder.standard()
          .withClientConfiguration(clientConfiguration)
          .withCredentials(new DefaultAWSCredentialsProviderChain());
    } else {
      return AmazonSQSClientBuilder.standard()
          .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(localstackUrl, region))
          .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
    }
  }

  @Bean
  default MessageConverter jacksonJmsMessageConverter() {
    return new DefaultTypeJackson2MessageConverter<>(VisitRequest.class);
  }

  @Bean
  default DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                         MessageConverter messageConverter,
                                                                         @Value("${messaging.jms.concurrency}") String concurrency) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setDestinationResolver(new DynamicDestinationResolver());
    factory.setConcurrency(concurrency);
    factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
    factory.setMessageConverter(messageConverter);

    return factory;
  }

  @Bean
  default ProviderConfiguration providerConfiguration(@Value("${messaging.sqs.numberOfMessagesToPrefetch}") int numberOfMessagesToPrefetch) {
    return new ProviderConfiguration().withNumberOfMessagesToPrefetch(numberOfMessagesToPrefetch);
  }

  @Bean
  default ClientConfiguration clientConfiguration(@Value("${messaging.sqs.maxConnections}") int maxConnections) {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.withConnectionTimeout(1000); // see com.amazonaws.services.sqs.AmazonSQSClientConfigurationFactory()
    clientConfiguration.withMaxConnections(maxConnections);

    return clientConfiguration;
  }

  @Bean
  default SQSConnectionFactory connectionFactory(ProviderConfiguration providerConfiguration, AmazonSQSClientBuilder clientBuilder) {
    return new SQSConnectionFactory(providerConfiguration, clientBuilder);
  }

}
