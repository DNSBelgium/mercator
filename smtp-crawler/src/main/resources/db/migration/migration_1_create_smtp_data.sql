--drop table smtp_crawler.smtp_data;

/*
create table smtp_crawl_result_per_month
as
select  min(id) min_id, max(id) max_id, count(1) rowcount, date_trunc('month', crawl_timestamp) as month
from smtp_crawler.smtp_crawl_result
group by date_trunc('month', crawl_timestamp);
*/
create table smtp_data
as
with min_max as (select min(min_id) as min_id, max(max_id) as max_id from smtp_crawl_result_per_month)--where month = '2021-06-29 00:00:00.000000 +00:00')
    , conversations as (select
                           id
                            , domain_name
                            , crawl_status
                            , visit_id
                            , jsonb_array_elements(servers) ->> 'hostName' as hostName
                            , jsonb_array_elements(servers) ->> 'priority' as priority
                            , r.crawl_timestamp as timestamp
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'ip' as ip
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'asn' as asn
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'country' as country
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'asnOrganisation' as asn_organisation
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'banner' as banner
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'connectOK' as connect_ok
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'connectReplyCode' as connect_reply_code
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'ipVersion' as ip_version
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'startTlsOk' as start_tls_ok
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'startTlsReplyCode' as start_tls_reply_code
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'errorMessage' as error_message
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'connectionTimeMs' as connection_time_ms
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'software' as software
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'softwareVersion' as software_version
                            , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') -> 'supportedExtensions' as extensions
                       from smtp_crawler.smtp_crawl_result r
                       where id between (select min_id from min_max) and (select max_id from min_max)
                       order by crawl_timestamp
)
select
      id
    , domain_name
    , crawl_status
    , visit_id
    , timestamp
    , hostName
    , cast(priority as int)
     , ip
     , cast(asn as bigint)
     , country
     , asn_organisation
     , banner
     , cast(connect_ok as boolean)
     , cast(connect_reply_code as int)
     , cast(ip_version as smallint)
     , cast(start_tls_ok as boolean)
     , cast(start_tls_reply_code as int)
     , error_message
     , cast(connection_time_ms as bigint)
     , software
     , software_version
     , extensions
from conversations;