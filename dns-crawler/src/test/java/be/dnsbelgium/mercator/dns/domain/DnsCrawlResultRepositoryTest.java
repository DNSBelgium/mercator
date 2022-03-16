package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.persistence.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.persistence.DnsCrawlResultRepository;
import be.dnsbelgium.mercator.dns.persistence.GeoIp;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.domain.resolver.RecordsTest;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  DnsCrawlResultRepository repository;

  @Test
  void findByVisitId() {
    UUID uuid = randomUUID();
    DnsCrawlResult dnsCrawlResult = new DnsCrawlResult(1L, uuid, "dnsbelgium.be", true, null, Map.of("@", RecordsTest.dnsBelgiumRootRecords()));
    dnsCrawlResult.addGeoIp(dnsBelgiumGeoIps());
    repository.save(dnsCrawlResult);

    DnsCrawlResult crawlResult = repository.findByVisitId(uuid).get();

    assertThat(crawlResult).isNotNull();
    assertThat(crawlResult.getAllRecords().get("@")).isEqualTo(RecordsTest.dnsBelgiumRootRecords());
    assertThat(crawlResult.getGeoIps()).containsExactlyInAnyOrderElementsOf(dnsBelgiumGeoIps());
  }

  // Object Mother
  public static List<GeoIp> dnsBelgiumGeoIps() {
    return List.of(
      new GeoIp(RecordType.A, "107.154.248.139", "US", Pair.of(19551, "INCAPSULA")),
      new GeoIp(RecordType.AAAA, "2a02:e980:53:0:0:0:0:8b", "US", Pair.of(19551, "INCAPSULA"))
    );
  }

}
