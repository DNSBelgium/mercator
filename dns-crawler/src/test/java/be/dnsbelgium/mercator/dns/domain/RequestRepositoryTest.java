package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
class RequestRepositoryTest {

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "dns_crawler");
  }

  @Autowired
  RequestRepository requestRepository;

  @Test
  void findByVisitId() {
    UUID visitId = randomUUID();
    Request request = Request.builder()
        .visitId(visitId)
        .domainName("dnsbelgium.be")
        .ok(true)
        .problem(null)
        .prefix("@")
        .recordType(RecordType.A)
        .rcode(0)
        .crawlTimestamp(ZonedDateTime.now())
        .build();

    Request request2 = Request.builder()
        .visitId(visitId)
        .domainName("google.be")
        .ok(true)
        .problem(null)
        .prefix("www")
        .recordType(RecordType.AAAA)
        .rcode(0)
        .crawlTimestamp(ZonedDateTime.now())
        .build();

    Request request3 = Request.builder()
        .visitId(randomUUID())
        .domainName("google.be")
        .ok(true)
        .problem(null)
        .prefix("www")
        .recordType(RecordType.A)
        .rcode(0)
        .crawlTimestamp(ZonedDateTime.now())
        .build();

    requestRepository.save(request);
    requestRepository.save(request2);
    requestRepository.save(request3);

    List<Request> requests = requestRepository.findRequestsByVisitId(visitId);

    assertFalse(requests.isEmpty());
    assertThat(requests).hasSize(2);
    assertThat(requests).containsExactlyInAnyOrder(request, request2);
  }

  @Test
  void findByVisitId_with_responses_and_geoips() {
    UUID visitId = randomUUID();
    Request request = Request.builder()
        .visitId(visitId)
        .domainName("dnsbelgium.be")
        .ok(true)
        .problem(null)
        .prefix("@")
        .recordType(RecordType.A)
        .rcode(0)
        .crawlTimestamp(ZonedDateTime.now())
        .build();

    // 1 Request has N Responses.
    Response r1 = Response.builder()
        .recordData("Some record data")
        .ttl(5000L)
        .build();
    Response r2 = Response.builder()
        .recordData("Some more record data")
        .ttl(5000L)
        .build();

    // Geo Ip for r1
    Pair<Integer, String> asn = Pair.of(1, "GROUP");
    ResponseGeoIp responseGeoIp = new ResponseGeoIp(asn, "BE", 4, "1.2.3.4");
    r1.getResponseGeoIps().add(responseGeoIp);

    request.setResponses(List.of(r1, r2));

    requestRepository.save(request);

    List<Request> requests = requestRepository.findRequestsByVisitId(visitId);

    assertThat(requests).hasSize(1);
    assertThat(requests.get(0).getResponses()).hasSize(2);

    assertThat(requests.get(0).getResponses().get(0)).isEqualTo(r1);
    assertThat(requests.get(0).getResponses().get(1)).isEqualTo(r2);

    assertThat(requests.get(0).getResponses().get(0).getResponseGeoIps()).hasSize(1);
    assertThat(requests.get(0).getResponses().get(0).getResponseGeoIps().get(0).getAsn()).isEqualTo(String.valueOf(asn.getLeft()));
    assertThat(requests.get(0).getResponses().get(0).getResponseGeoIps().get(0).getCountry()).isEqualTo("BE");

    assertThat(requests.get(0).getResponses().get(1).getResponseGeoIps()).hasSize(0);

  }
}
