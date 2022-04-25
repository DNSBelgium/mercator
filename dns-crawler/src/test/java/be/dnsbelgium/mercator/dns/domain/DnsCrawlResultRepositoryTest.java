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
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
class DnsCrawlResultRepositoryTest {

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "dns_crawler");
  }

  @Autowired
  RequestRepository requestRepository;

  @Test
  void findByVisitId() { // TODO: AvR Update to use Request. Null issue as well.
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

    Request saved = requestRepository.save(request); // Breaks here. Something's wrong with the repositories.

    Request found = requestRepository.findRequestByVisitId(uuid).get();

//    assertThat(found).isNotNull();
//    assertThat(crawlResult.getAllRecords().get("@")).isEqualTo(RecordsTest.dnsBelgiumRootRecords());
//    assertThat(crawlResult.getGeoIps()).containsExactlyInAnyOrderElementsOf(dnsBelgiumGeoIps());
  }

  // TODO: AvR Update to new ResponseGeoIp
  // Object Mother
//  public static List<GeoIp> dnsBelgiumGeoIps() {
//    return List.of(
//      new GeoIp(RecordType.A, "107.154.248.139", "US", Pair.of(19551, "INCAPSULA")),
//      new GeoIp(RecordType.AAAA, "2a02:e980:53:0:0:0:0:8b", "US", Pair.of(19551, "INCAPSULA"))
//    );
//  }

}
