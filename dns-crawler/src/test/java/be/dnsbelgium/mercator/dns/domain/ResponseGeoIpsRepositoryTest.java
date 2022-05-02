package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolutionTest;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.*;
import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.xbill.DNS.Name;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
public class ResponseGeoIpsRepositoryTest {

    @Container
    static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        container.setDatasourceProperties(registry, "dns_crawler");
    }

    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private ResponseRepository responseRepository;
    @Autowired
    private ResponseGeoIpRepository responseGeoIpRepository;

    @Test
    void findAllByResponseId() {
        UUID visitId = randomUUID();
        Request request = Request.builder()
                .id(1L)
                .visitId(visitId)
                .domainName("dnsbelgium.be")
                .ok(true)
                .problem(null)
                .prefix("@")
                .recordType(RecordType.A)
                .rcode(0)
                .crawlTimestamp(ZonedDateTime.now())
                .build();

        Request savedRequest = requestRepository.save(request);

        Response r1 = Response.builder()
                .id(1L)
                .recordData("194.35.35.35")
                .ttl(5000)
                .request(savedRequest)
                .build();

        Response savedResponse = responseRepository.save(r1);

        // 1 Response has N ResponseGeoIps
        Pair<Integer, String> asn = Pair.of(1, "GROUP");
        ResponseGeoIp responseGeoIp = new ResponseGeoIp(asn, "BE", 4, savedResponse);

        responseGeoIpRepository.save(responseGeoIp);

        List<ResponseGeoIp> responseGeoIps = responseGeoIpRepository.findAllByResponseRequestVisitId(visitId);

        assertFalse(responseGeoIps.isEmpty());
        assertThat(responseGeoIp).isEqualTo(responseGeoIps.get(0));
    }
}
