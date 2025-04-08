package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component
public class DnsRepository extends BaseRepository<DnsCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(DnsRepository.class);

    private final String requestDestination;
    private final String responseDestination;
    private final String geoIpDestination;


    @SneakyThrows
    public DnsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, DnsCrawlResult.class);
        String subPath = "dns";
        requestDestination = createDestination(baseLocation, subPath, "requests");
        responseDestination = createDestination(baseLocation, subPath, "responses");
        geoIpDestination = createDestination(baseLocation, subPath, "geoips");
    }

    @Override // the application will throw an error if it tries to execute this without a geoip parquet file present
    public String getAllItemsQuery() {
        return StringSubstitutor.replace("""
                with
                    dns_geoip_response as (
                        select *  from read_parquet('${geoIpResponseDestination}/**/*.parquet')
                    ),
                    dns_response as (
                        select * from read_parquet('${responseDestination}/**/*.parquet')
                    ),
                    dns_request as (
                        select *  from read_parquet('${requestDestination}/**/*.parquet')
                    ),
                        geo_ip as (
                            select
                                visit_id,
                                response_id,
                                list(
                                   struct_pack(
                                           asn := dns_geoip_response.asn,
                                           country := dns_geoip_response.country,
                                           ip := dns_geoip_response.ip,
                                           asn_organisation := dns_geoip_response.asn_organisation,
                                           ip_version := dns_geoip_response.ip_version
                                   )
                               ) as response_geo_ips
                            from dns_geoip_response
                            group by visit_id, response_id
                        ),
                        responses_with_geoip as (
                            select
                                dns_response.visit_id,
                                dns_response.request_id,
                                struct_pack(
                                           dns_response.response_id,
                                           dns_response.record_data,
                                           dns_response.ttl,
                                           geo_ip.response_geo_ips
                                ) as response
                            from dns_response
                            left join geo_ip
                                on dns_response.visit_id = geo_ip.visit_id
                                and dns_response.response_id = geo_ip.response_id
                        ),
                        response_list as (
                            select
                                visit_id,
                                request_id,
                                list(response order by crawl_timestamp, request_id) as responses
                            from responses_with_geoip
                            group by visit_id, request_id
                        ),
                        requests as (
                            select dns_request.* exclude(year, month, status), responses
                            from dns_request
                            join response_list
                                on  response_list.visit_id = dns_request.visit_id
                                and response_list.request_id = dns_request.request_id
                        ),
                        dnsCrawlResult as (
                            select
                                'OK' as status,
                                visit_id,
                                domain_name,
                                min(crawl_timestamp) as crawl_timestamp,
                                list(requests order by request_id) as requests
                            from requests
                            group by visit_id, domain_name
                        )
                    select * from dnsCrawlResult
                """, Map.of(
                        "geoIpResponseDestination", this.geoIpDestination,
                        "responseDestination", this.responseDestination,
                        "requestDestination", this.requestDestination
        ));
    }

    /*
     * This method will use the passed in CTE definitions and the name of a specific CTE to copy the data to the given destination
     */
    private void copyToParquet(String jsonLocation, DataSource dataSource, String cteDefinitions, String cte, String destination) {
        String copyStatement = StringSubstitutor.replace("""
                COPY (
                    ${cteDefinitions}
                    select * from ${cte}
                ) TO '${destination}'
                  (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN '{uuid}')
                """, Map.of(
                        "cteDefinitions", cteDefinitions,
                        "cte", cte,
                        "destination", destination
                )
        );
        logger.debug("cte={}, copyStatement=\n{}", cte, copyStatement);
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("set variable jsonLocation = ?")
                .param(jsonLocation)
                .update();
        jdbcClient.sql(copyStatement).update();
        logger.info("copying {} as parquet to {} done", cte, destination);
    }

    @Override
    public void storeResults(String jsonLocation) {
        try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
            String cteDefinitions = readFromClasspath("sql/dns/cte_definitions.sql");
            logger.debug("cteDefinitions: {}", cteDefinitions);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "requests", requestDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "responses", responseDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "geo_ips_casted", geoIpDestination);
        }
    }

}
