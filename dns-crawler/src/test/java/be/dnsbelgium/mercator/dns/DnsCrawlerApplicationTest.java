package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.persistence.*;
import be.dnsbelgium.mercator.dns.ports.DnsCrawler;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ActiveProfiles({"local", "test"})
@SpringBootTest
public class DnsCrawlerApplicationTest {

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

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

  @Autowired
  DnsCrawler dnsCrawler;

  @Autowired
  RequestRepository requestRepository;
  @Autowired
  ResponseRepository responseRepository;

  @MockBean
  ResponseGeoIpRepository responseGeoIpRepository;

  @Test
  public void idn() throws Exception {
    // This domain is not under our control, so test might fail in the future
    String domainName = "café.be";
    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), domainName);
    dnsCrawler.process(visitRequest);

    List<Request> requests = requestRepository.findRequestsByVisitId(visitRequest.getVisitId());
    assertFalse(requests.isEmpty());
    assertThat(requests.get(0).getProblem()).isNotEqualTo("nxdomain");

    List<Response> responses = responseRepository.findAllByRequestVisitId(requests.get(0).getVisitId());
    assertFalse(responses.isEmpty());

    // In theory the following code works, but I cannot set geoIpEnabled to true due to not having a Maxmind license key for testing.
//    List<ResponseGeoIp> responseGeoIps = responseGeoIpRepository.findAllByResponseRequestVisitId(requests.get(0).getVisitId());
//    assertFalse(responseGeoIps.isEmpty());
  }

}
