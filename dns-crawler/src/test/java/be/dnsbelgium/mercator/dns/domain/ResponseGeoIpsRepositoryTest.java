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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
public class ResponseGeoIpsRepositoryTest {
    // autocomplete templates
    private static final Logger logger = LoggerFactory.getLogger(ResponseGeoIpsRepositoryTest.class);

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

    @MockBean
    GeoIPService geoIPService;

    @Test
    void findAllByResponseId() {
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

        Request savedRequest = requestRepository.save(request);

        Response r1 = new Response.Builder()
                .id(1L)
                .recordData("194.35.35.35")
                .ttl(5000)
                .request(savedRequest)
                .build();

        Response savedResponse = responseRepository.save(r1);

        logger.info("geoIPService: {}", geoIPService);

        // When geoIpService lookup country, return new Pair.
        // TODO: Finish this test.
        // 1 Response has N ResponseGeoIps


//        Optional<Pair<Integer, String>> asn = geoIPService.lookupASN("8.8.8.8"); // Returns Optional.empty();
//        when(geoIPService.lookupASN("8.8.8.8")).thenReturn(new Pair<Integer, String>(1, "GROUP")); // Cannot instantiate abstract.
//
//        // I somehow need an asn to give to responseGeoIp, otherwise I cannot save it to the DB.
//        ResponseGeoIp responseGeoIp = new ResponseGeoIp(4, savedResponse.getRecordData(), "BE", asn.get());


//        responseGeoIpRepository.save(responseGeoIp);
//
//        List<ResponseGeoIp> responseGeoIps = responseGeoIpRepository.findAllByResponseId(savedResponse.getId());
//        assertFalse(responseGeoIps.isEmpty());
    }
}
