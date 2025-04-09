with
    unnested as (
        select status, unnest(requests, max_depth:=2)
        from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/dns/*.json'), field_appearance_threshold = 1)
    ),
    all_results as (
        select
            *,
            year(to_timestamp(crawl_timestamp))     as year,
            month(to_timestamp(crawl_timestamp))    as month
        from unnested
    ),
    requests as (
        select
            status             ::VARCHAR       as status,
            request_id         ::VARCHAR       as request_id,
            visit_id           ::VARCHAR       as visit_id,
            domain_name        ::VARCHAR       as domain_name,
            prefix             ::VARCHAR       as prefix,
            record_type        ::VARCHAR       as record_type,
            rcode              ::INTEGER       as rcode,
            to_timestamp(crawl_timestamp)      as crawl_timestamp,
            ok                 ::BOOLEAN       as ok,
            problem            ::VARCHAR       as problem,
            num_of_responses   ::INTEGER       as num_of_responses,
            year,
            month
        from all_results
    ),
    responses_unnested as (
        select
            request_id,
            visit_id,
            unnest(responses, max_depth := 2),
            year,
            month
        from all_results
    ),
    responses as (
        select
            request_id          ::VARCHAR      as request_id,
            visit_id            ::VARCHAR      as visit_id,
            response_id         ::VARCHAR      as response_id,
            record_data         ::VARCHAR      as record_data,
            ttl                 ::INTEGER      as ttl,
            year                ::INTEGER      as year,
            month               ::INTEGER      as month,
            response_geo_ips
        from responses_unnested
    ),
    geo_ips as (
        select
            visit_id,
            request_id,
            response_id,
            unnest(response_geo_ips, max_depth := 2),
            year,
            month
        from responses
    ),
    geo_ips_casted as (
        select
            visit_id            ::VARCHAR       as visit_id,
            request_id          ::VARCHAR       as request_id,
            response_id         ::VARCHAR       as response_id,
            asn                 ::VARCHAR       as asn,
            country             ::VARCHAR       as country,
            ip                  ::VARCHAR       as ip,
            asn_organisation    ::VARCHAR       as asn_organisation,
            ip_version          ::VARCHAR       as ip_version,
            year,
            month
        from geo_ips
    )
