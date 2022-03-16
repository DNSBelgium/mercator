package be.dnsbelgium.mercator.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
public class Config {

  private static final Logger logger = getLogger(Config.class);

  @Bean
  @Profile("local")
  @Primary
  public AmazonSQSAsync localAmazonSQSClientBuilder(@Value("${localstack.url}") String localstackUrl) {
    logger.info("creating a AmazonSQSAsync client using localstack at {}", localstackUrl);
    return AmazonSQSAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(localstackUrl, "eu-west-1"))
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("accesskey", "secretkey")))
        .build();
  }

  @Bean
  @Profile({"!local"})
  @Primary
  public AmazonSQSAsync amazonSQSClientBuilder() {
    logger.info("creating a real AmazonSQSAsync client");
    AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
    return AmazonSQSAsyncClientBuilder
        .standard()
        .withCredentials(credentialsProvider)
        .build();
  }

}
