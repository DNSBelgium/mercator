package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"local", "test"})
@SpringBootTest
public class DnsCrawlerApplicationTest {

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlerApplicationTest.class);
  
  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "dns_crawler");
    localstack.setDynamicPropertySource(registry);
  }

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-dns-crawler-input");
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-dispatcher-ack");
  }

  @Test
  public void loadApplicationContext() {
  }

  @Test void dispatch_ulabel() throws IOException, InterruptedException {
    dispatch("dnsbelgië.be");
  }

  @Test void dispatch_alabel() throws IOException, InterruptedException {
    dispatch("xn--dnsbelgi-01a.be");
  }

  void dispatch(String domainName) throws IOException, InterruptedException {
    UUID visitId = UUID.randomUUID();
    String request = String.format("{\"visitId\": \"%s\", \"domainName\": \"%s\"}", visitId, domainName);
    logger.info("request: {}", request);
    var result = localstack.execInContainer(
        "awslocal",
        "sqs", "send-message",
        "--queue-url", "http://localhost:4566/000000000000/mercator-dns-crawler-input",
        "--message-body", request);

    logger.info("result.stderr = " + result.getStderr());
    logger.info("result.stdout = " + result.getStdout());

    List<Request> found = requestRepository.findByVisitId(visitId);
    var retries = 0;
    while (found.isEmpty() && retries++ < 20) {
      found = requestRepository.findByVisitId(visitId);
      Thread.sleep(200);
    }
    logger.info("We found in the database: {}", found);
    assertThat(found).isNotEmpty();
    assertThat(found.get(0).getDomainName()).isEqualTo("dnsbelgië.be");
  }

  @Autowired
  RequestRepository requestRepository;

}
