package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DnsRepository extends BaseRepository<DnsCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(DnsRepository.class);

    private final String dnsRequestDestination;
    private final String dnsResponseDestination;
    private final String dnsGeoIpResponseDestination;


    @SneakyThrows
    public DnsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, DnsCrawlResult.class);
        String subPath = "dns";
        dnsRequestDestination = createDestination(baseLocation, subPath, "requests");
        dnsResponseDestination = createDestination(baseLocation, subPath, "responses");
        dnsGeoIpResponseDestination = createDestination(baseLocation, subPath, "response_geoips");
    }

    @Override // the application will throw an error if it tries to execute this without a geoip parquet file present
    public String getAllItemsQuery() {
        return StringSubstitutor.replace("""
                with
                    dns_geoip_response as (
                        select *  from read_parquet('${dnsGeoIpResponseDestination}/**/*.parquet')
                    ),
                    dns_response as (
                        select * from read_parquet('${dnsResponseDestination}/**/*.parquet')
                    ),
                    dns_requests as (
                        select *  from read_parquet('${dnsRequestDestination}/**/*.parquet')
                    ),
                    geoips_list as (
                        select dns_response.visit_id,
                               dns_response.record_data,
                               dns_response.request_id,
                               dns_response.ttl,
                               list(
                                    struct_pack(
                                        asn := dns_geoip_response.asn,
                                        country := dns_geoip_response.country,
                                        ip := dns_geoip_response.ip,
                                        asn_organisation := dns_geoip_response.asn_organisation,
                                        ip_version := dns_geoip_response.ip_version
                                    )
                
                               ) as response_geo_ips
                        from  dns_response
                        LEFT join dns_geoip_response on dns_geoip_response.response_id = dns_response.id
                        group by dns_response.visit_id, dns_response.record_data, dns_response.request_id, dns_response.ttl
                    ),
                    rename as (
                        select
                            request_id as id,
                            record_data,
                            ttl,
                            response_geo_ips
                        from  geoips_list
                    ),
                    responses_list as (
                        select dns_requests.* EXCLUDE (year, month), list(rename) as responses
                        from dns_requests
                            LEFT join rename
                                on rename.id = dns_requests.id
                        group by all
                
                    ),
                    dnsCrawlResult as (
                        select
                            status,
                            visit_id,
                            domain_name,
                            min(crawl_timestamp) as crawl_timestamp,
                            -- this part is done by using a subquery to ensure the order of the requests array on dnsCrawlResult
                            (
                                select list(
                                            struct_pack(
                                                 id := responses_list.id,
                                                 visit_id := responses_list.visit_id,
                                                 domain_name := responses_list.domain_name,
                                                 prefix := responses_list.prefix,
                                                 record_type := responses_list.record_type,
                                                 rcode := responses_list.rcode,
                                                 crawl_timestamp := responses_list.crawl_timestamp,
                                                 ok := responses_list.ok,
                                                 problem := responses_list.problem,
                                                 num_of_responses := responses_list.num_of_responses,
                                                 responses := responses_list.responses
                                         )
                                        order by responses_list.crawl_timestamp, responses_list.id
                                    )
                                from responses_list
                            ) as requests
                        from responses_list
                        group by status, visit_id, domain_name
                    )
                select * from dnsCrawlResult
                """, Map.of(
                        "dnsGeoIpResponseDestination", this.dnsGeoIpResponseDestination,
                        "dnsResponseDestination", this.dnsResponseDestination,
                        "dnsRequestDestination", this.dnsRequestDestination
        ));
    }


    @Override
    public void storeResults(String jsonResultsLocation) {
        String allResultsQuery = StringSubstitutor.replace("""
          with unnested as (
            select status, unnest(requests, max_depth:=2)
            from read_json('${jsonFile}', field_appearance_threshold = 1)
          ),
          all_results as (
        
              select *,
                      year(to_timestamp(${timestampField})) as year,
                      month(to_timestamp(${timestampField})) as month
              from unnested
          ),
          """, Map.of(
                  "timestampField", this.timestampField(),
                      "jsonFile", jsonResultsLocation
        ));

        logger.info("allResultsQuery: {}", allResultsQuery);

        String copyDnsRequests = StringSubstitutor.replace("""
                COPY(
                        ${allResultsQuery}
                         cast_datatypes as (
                             select
                                 status as status,
                                 id::VARCHAR as id,
                                     visit_id::VARCHAR as visit_id,
                                     domain_name::VARCHAR as domain_name,
                                     prefix::VARCHAR as prefix,
                                     record_type::VARCHAR as record_type,
                                     rcode::INTEGER as rcode,
                                     crawl_timestamp::DOUBLE as crawl_timestamp,
                                     ok::BOOLEAN as ok,
                                     problem::VARCHAR as problem,
                                     num_of_responses::INTEGER as num_of_responses,
                                     year,
                                     month
                             from
                                 all_results
                         )
                    select * from cast_datatypes
                ) TO '${dnsRequestDestination}' (FORMAt parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN '{uuid}')
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                        "dnsRequestDestination", this.dnsRequestDestination
        ));
        logger.info("copyDnsCrawlResult: {}", copyDnsRequests);

        getJdbcClient().sql(copyDnsRequests).update();

        String copyDnsResponses = StringSubstitutor.replace("""
                COPY (
                    ${allResultsQuery}
                    responses_unnested as (
                        select id as request_id, visit_id, unnest(responses, max_depth := 2), year, month from all_results
                    ),
                    responses as (
                        select * exclude (response_geo_ips) from responses_unnested
                    ),
                    cast_datatypes as (
                        select
                            request_id::VARCHAR as request_id,
                            visit_id::VARCHAR as visit_id,
                            id::VARCHAR as id,
                            record_data as record_data,
                            ttl as ttl,
                            year,
                            month
                        from responses
                    )
                select * from cast_datatypes
                ) TO '${dnsResponseDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN '{uuid}')
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                "dnsResponseDestination", this.dnsResponseDestination
        ));
        logger.info("copyDnsCrawlResult: {}", copyDnsResponses);
        getJdbcClient().sql(copyDnsResponses).update();


        String copyDnsGeoipResponses = StringSubstitutor.replace("""
                COPY (
                    ${allResultsQuery}
                    responses as (
                        select visit_id, id as request_id, unnest(responses, max_depth := 2) as responses, year, month  from all_results
                    ),
                    geoIps as (
                        select visit_id, request_id, id as response_id, unnest(response_geo_ips, max_depth := 2 ), year, month from responses
                    ),
                    cast_datatypes as (
                        select
                            visit_id::VARCHAR as visit_id,
                            request_id::VARCHAR  as request_id,
                            response_id::VARCHAR  as response_id,
                            asn::VARCHAR  as asn,
                            country::VARCHAR  as country,
                            ip::VARCHAR  as ip,
                            asn_organisation::VARCHAR  as asn_organisation,
                            ip_version::VARCHAR  as ip_version,
                            year,
                            month
                        from geoIps
                
                    )
                select * from cast_datatypes
                
                ) TO '${dnsGeoIpResponseDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN '{uuid}')
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                "dnsGeoIpResponseDestination", this.dnsGeoIpResponseDestination
        ));
        logger.info("copyDnsGeoipResponse: {}", copyDnsGeoipResponses);
        getJdbcClient().sql(copyDnsGeoipResponses).update();
    }


}
