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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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
    public void testConvertDnsCrawlResultToJsonToParquet() throws IOException {

        VisitRequest visitRequest = new VisitRequest("dnsbelgium.be");

        DnsCrawlResult result = service.retrieveDnsRecords(visitRequest);
        String json = mapper.writeValueAsString(result);

        objectMapper.writeValue(new File("dns-data.json"), result);

        Path p = Paths.get("dns-data.json");



        String query = String.format(
                """
            
            WITH save_parq as (
    
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
                FROM '%s' r
                ) AS unnested_requests,
                LATERAL unnest(request.responses) AS response
            ORDER BY record_type;
            )
            
            
            COPY
                (SELECT * FROM save_parq)
                TO 'dns-crawl-result.parquet'
                (FORMAT PARQUET);
                
    """, p);
        jdbcClient.sql(query);

        System.out.println("done.. ");



    }
}