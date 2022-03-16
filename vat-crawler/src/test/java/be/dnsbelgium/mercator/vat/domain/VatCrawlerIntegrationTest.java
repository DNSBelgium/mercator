package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.vat.ports.VatCrawler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.UUID;

@Disabled("since this test depends on internet access and on the state of the sites it tries to visit")
@SpringBootTest
@ActiveProfiles({"local", "test"})
public class VatCrawlerIntegrationTest {

  // Integration Test for locally debugging websites that lead to unexpected failures.
  // This test does not use mocks: it uses the real VatCrawler to visit websites
  // and stores the result in a Postgres database.
  // LocalStack is started to avoid complaints about unexisting SQS queues

  @Autowired
  private VatCrawler vatCrawler;

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "vat_crawler");
    localstack.setDynamicPropertySource(registry);
  }

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-vat-crawler-input");
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-dispatcher-ack");
  }

  @Test
  public void casino() {
    UUID visitId = UUID.randomUUID();
    vatCrawler.process(new VisitRequest(visitId, "casino-legal-belgique.be"));
  }

}
