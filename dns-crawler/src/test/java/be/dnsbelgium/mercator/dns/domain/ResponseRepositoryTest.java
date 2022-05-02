package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.dns.persistence.ResponseRepository;
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
class ResponseRepositoryTest {

    @Container
    static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        container.setDatasourceProperties(registry, "dns_crawler");
    }

    @Autowired
    RequestRepository requestRepository;
    @Autowired
    ResponseRepository responseRepository;

    @Test
    void findAllByRequestId() {
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

        // 1 Request has N Responses.
        Response r1 = Response.builder()
                .id(1L)
                .recordData("Some record data")
                .ttl(5000)
                .request(savedRequest)
                .build();
        Response r2 = Response.builder()
                .id(2L)
                .recordData("Some more record data")
                .ttl(5000)
                .request(savedRequest)
                .build();

        responseRepository.save(r1);
        responseRepository.save(r2);

        List<Response> responses = responseRepository.findAllByRequestVisitId(visitId);
        assertFalse(responses.isEmpty());

        assertThat(r1).isEqualTo(responses.get(0));
        assertThat(r2).isEqualTo(responses.get(1));
    }

}
