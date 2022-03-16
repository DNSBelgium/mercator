package be.dnsbelgium.mercator.feature.extraction;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
public class S3Config {

  private static final Logger logger = getLogger(S3Config.class);

  @Bean
  @Primary  // mark as Primary to avoid conflicts with the AmazonS3 created by spring-aws-auto-configure
  public AmazonS3 amazonS3(ClientConfiguration clientConfiguration,
                                 @Value("${localstack.url:}") String localstackUrl,
                                 @Value("${cloud.aws.credentials.accessKey:accesskey}") String accessKey,
                                 @Value("${cloud.aws.credentials.secretKey:secretkey}") String secretKey,
                                 @Value("${cloud.aws.region.static:eu-west-1}") String region) {
    if (localstackUrl.isEmpty()) {
      logger.info("localstack.url not set => using DefaultAWSCredentialsProviderChain to create AmazonS3ClientBuilder");

      return AmazonS3ClientBuilder.standard()
          .withClientConfiguration(clientConfiguration)
          .withCredentials(new DefaultAWSCredentialsProviderChain())
          .withPathStyleAccessEnabled(true)
          .build();
    } else {
      logger.info("localstack.url={} => using AWSStaticCredentialsProvider to create AmazonS3ClientBuilder", localstackUrl);
      logger.info("region={} accessKey={} secretKey={}", region, accessKey, secretKey);
      return AmazonS3ClientBuilder.standard()
          .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(localstackUrl, region))
          .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
          // To avoid domain name resolution issues, you need to enable path style access
          // see https://github.com/localstack/localstack/blob/master/README.md
          .withPathStyleAccessEnabled(true)
          .build();
    }
  }

  @Bean
  public ClientConfiguration clientConfig() {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.withConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT);
    logger.info("created ClientConfiguration withConnectionTimeout = {} ms", clientConfiguration.getConnectionTimeout());
    return clientConfiguration;
  }

}
