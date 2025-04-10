with
    dns_geoip_response as (
        select *
        from read_parquet(coalesce(getvariable('geoIpDestination'), '~/mercator/data/dns/geoips') || '/**/*.parquet')
    ),
    dns_response as (
        select *
        from read_parquet(coalesce(getvariable('responseDestination'), '~/mercator/data/dns/responses') || '/**/*.parquet')
    ),
    dns_request as (
        select * replace(epoch(crawl_timestamp) as crawl_timestamp)
        from read_parquet(coalesce(getvariable('requestDestination'), '~/mercator/data/dns/requests') || '/**/*.parquet')
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