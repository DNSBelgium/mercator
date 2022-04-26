package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
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

  @Test // Multiple requests have the same VisitId.
  void findByVisitId() {
    UUID uuid = randomUUID();
    Request request = new Request.Builder()
            .id(1L)
            .visitId(uuid)
            .domainName("dnsbelgium.be")
            .ok(true)
            .problem(null)
            .prefix("@")
            .recordType(RecordType.A)
            .rcode(0)
            .crawlTimestamp(ZonedDateTime.now())
            .build();

    requestRepository.save(request);
    List<Request> requests = requestRepository.findRequestsByVisitId(uuid);

    assertFalse(requests.isEmpty());
    assertThat(request).isEqualTo(requests.get(0));
  }

}
