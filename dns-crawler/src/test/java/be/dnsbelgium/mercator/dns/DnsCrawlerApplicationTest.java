package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.*;
import be.dnsbelgium.mercator.dns.ports.DnsCrawler;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

  @Test
  public void idn() throws Exception {
    // This domain is not under our control, so test might fail in the future
    String domainName = "caf??.be";
    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), domainName);
    dnsCrawler.process(visitRequest);

    List<Request> requests = requestRepository.findByVisitId(visitRequest.getVisitId());
    assertThat(requests).hasSize(16);

    Request soa = requests.stream().filter(request -> request.getRecordType() == RecordType.SOA).findFirst().get();
    Request a = requests.stream().filter(request -> request.getRecordType() == RecordType.A).findFirst().get();
    assertThat(soa.getResponses()).hasSize(1);
    assertThat(a.getResponses()).hasSize(1);

    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.A))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.AAAA))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.MX))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.SOA))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.TXT))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.CAA))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.HTTPS))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.SVCB))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.NS))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.DS))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.DNSKEY))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.CDNSKEY))).isTrue();
    assertThat(requests.stream().anyMatch(r -> r.getRecordType().equals(RecordType.CDS))).isTrue();

  }

}
