package be.dnsbelgium.mercator.dns.domain.dataConverter;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlService;
import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.duckdb.DuckDBConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DnsDataConverterTest {

    @Autowired
    DnsCrawlService service;

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final JdbcClient jdbcClient;

    public DnsDataConverterTest() {
        this.jdbcClient = JdbcClient.create(DuckDataSource.memory());
    }
    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void testRetrieveDnsCrawlResult() {
        VisitRequest visitRequest = new VisitRequest("dnsbelgium.be");

        DnsCrawlResult result = service.retrieveDnsRecords(visitRequest);
        System.out.println(result);
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(DnsCrawlResult.class);
    }

    @Test
    public void testConvertDnsCrawlResultToJsonToParquet() throws JsonProcessingException {

        VisitRequest visitRequest = new VisitRequest("dnsbelgium.be");

        DnsCrawlResult result = service.retrieveDnsRecords(visitRequest);
        String json = mapper.writeValueAsString(result);
        System.out.println(json);

        String query = String.format(
                """
    
            SELECT
                request.visitId as visit_id,
                request.crawlTimestamp as crawl_timestamp,
                request.domainName as domain_name,
                request.prefix,
                request.recordType as record_type,
                response->'unnest'->>'id' AS id,
                response->'unnest'->>'recordData' AS record_data,
                response->'unnest'->>'ttl' AS ttl
            FROM (
                SELECT unnest(requests) AS request
                FROM %s r
                ) AS unnested_requests,
                LATERAL unnest(request.responses) AS response
            ORDER BY record_type;
                
    """, json);
        String returnst = jdbcClient.sql(query).query().toString();

        System.out.println(returnst);



    }
}