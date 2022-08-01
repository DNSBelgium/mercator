package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeaturesRepository;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@ActiveProfiles({"local", "test"})
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeatureCrawlerTest {

  @Autowired FeatureService featureService;
  @Autowired Environment environment;
  @Autowired FeatureCrawler featureCrawler;

  @Autowired ContentCrawlResultRepository contentCrawlResultRepository;
  @Autowired HtmlFeaturesRepository htmlFeaturesRepository;

  @Container static LocalstackContainer localstack = new LocalstackContainer();
  @Container static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Autowired AmazonSQSClientBuilder sqsClientBuilder;

  private static final Logger logger = getLogger(FeatureCrawlerTest.class);

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    logger.info("pgsql = {}", pgsql);
    localstack.setDynamicPropertySource(registry);
    pgsql.setDatasourceProperties(registry, "features");
    logger.info("pgsql.getJdbcUrl() = {}", pgsql.getJdbcUrl());
    registry.add("spring.datasource.content.url",      () -> pgsql.getJdbcUrl() + "&currentSchema=" + "content_crawler");
    registry.add("spring.datasource.content.username", () -> pgsql.getUsername());
    registry.add("spring.datasource.content.password", () -> pgsql.getPassword());
  }

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-dispatcher-output");
  }

  @BeforeEach
  void beforeEach() throws IOException, InterruptedException {
    String queueName = environment.getProperty("feature.extraction.input.queue.name");
    logger.info("queueName = {}", queueName);
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queueName);
  }

  @Test
  public void loadApplicationContext() {
  }

  @Test
  public void getQueueUrl() {
    String queueName = environment.getProperty("feature.extraction.input.queue.name");
    AmazonSQS sqs = sqsClientBuilder.build();
    // this fails when we have conflicting library versions on the classpath
    String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
    logger.info("queueUrl = {}", queueUrl);
    assertThat(queueUrl).isNotNull();
  }

}
