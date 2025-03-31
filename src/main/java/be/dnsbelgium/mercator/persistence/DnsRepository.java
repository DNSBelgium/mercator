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

    private final String dnsCrawlDestination;
    private final String dnsResponseDestination;
    private final String dnsGeoIpResponseDestination;


    @SneakyThrows
    public DnsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, DnsCrawlResult.class);
        String subPath = "dns";
        dnsCrawlDestination = createDestination(baseLocation, subPath, "dns_crawl_result");
        dnsResponseDestination = createDestination(baseLocation, subPath, "dns_response");
        dnsGeoIpResponseDestination = createDestination(baseLocation, subPath, "dns_geoip_response");
    }

    @Override
    public String getAllItemsQuery() {
        return StringSubstitutor.replace("""
                with dns_geoip_response_result as (
                    select *  from read_parquet('${dnsGeoIpResponseDestination}/**/*.parquet')
                ),
                dns_response_result as (
                    select * from read_parquet('${dnsResponseDestination}/**/*.parquet')
                ),
                dns_crawl_result as (
                    select *  from read_parquet('${dnsCrawlDestination}/**/*.parquet')
                ),
                response_has_geoips as (
                    select resps.*, COALESCE(LIST(
                                        struct_pack(
                                            asn := geoipResps.asn,
                                            country := geoipResps.country,
                                            ip := geoipResps.ip,
                                            asn_organisation := geoipResps.asn_organisation,
                                            ip_version := geoipResps.ip_version
                                        )
                                    ), []) as response_geo_ips
                    from dns_geoip_response_result geoipResps left join dns_response_result resps on geoipResps.response_id = resps.id
                    group by all
                 ),
                rename as (
                    select request_id as id, record_data, ttl, response_geo_ips from response_has_geoips
                ),
                unnest_requests as (
                     select unnest(requests) from dns_crawl_result
                 ),
                request_has_responses as (
                    select reqs.*, COALESCE(list(resps), []) as responses
                    from unnest_requests reqs inner join rename resps
                        on resps.id = reqs.id
                    group by all
            
                ),
                 dns_crawl_result_has_requests as (
                     select dcr.status, dcr.visit_id, dcr.domain_name, dcr.crawl_timestamp, COALESCE(list(reqs), []) as requests
                     from dns_crawl_result dcr inner join request_has_responses reqs
                              on dcr.visit_id = reqs.visit_id
                    group by all
                 )
               
                
                select * from dns_crawl_result_has_requests
                
                
                """, Map.of(
                        "dnsGeoIpResponseDestination", this.dnsGeoIpResponseDestination,
                        "dnsResponseDestination", this.dnsResponseDestination,
                        "dnsCrawlDestination", this.dnsCrawlDestination
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

        String copyDnsCrawlResult = StringSubstitutor.replace("""
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
                         ),
                         dns_crawl_result as (
                             select status, domain_name, visit_id, crawl_timestamp, struct_pack(
                                     id := id,
                                     visit_id := visit_id,
                                     domain_name := domain_name,
                                     prefix := prefix,
                                     record_type := record_type,
                                     rcode := rcode,
                                     crawl_timestamp := crawl_timestamp,
                                     ok := ok,
                                     problem := problem,
                                     num_of_responses := num_of_responses) as requests,
                                     year,
                                     month
                             from cast_datatypes
                         )
                    select * from dns_crawl_result
                ) TO '${dnsCrawlDestination}' (FORMAt parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'dns_{uuid}')
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                        "dnsCrawlDestination", this.dnsCrawlDestination
        ));
        logger.info("copyDnsCrawlResult: {}", copyDnsCrawlResult);

        getJdbcClient().sql(copyDnsCrawlResult).update();

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
                ) TO '${dnsResponseDestination}' (FORMAt parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'dns_responses_{uuid}')
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
                
                ) TO '${dnsGeoIpResponseDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'dns_geoip_responses_{uuid}')
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                "dnsGeoIpResponseDestination", this.dnsGeoIpResponseDestination
        ));
        logger.info("copyDnsGeoipResponse: {}", copyDnsGeoipResponses);
        getJdbcClient().sql(copyDnsGeoipResponses).update();


    }


}
