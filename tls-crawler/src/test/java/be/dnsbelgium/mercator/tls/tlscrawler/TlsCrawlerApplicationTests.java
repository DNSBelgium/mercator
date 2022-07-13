package be.dnsbelgium.mercator.tls.tlscrawler;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "local"})
class TlsCrawlerApplicationTests {

  @Autowired
  TlsCrawler tlsCrawler;

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-tls-crawler-input");
  }


  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "tls_crawler");
    localstack.setDynamicPropertySource(registry);
  }

  @Test
  void contextLoads() {
  }

  @Test
  public void process() {
    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "google.be");
    tlsCrawler.process(visitRequest);
  }
}
