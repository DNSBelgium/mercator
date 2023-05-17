drop table smtp_crawler.smtp_data;

create table smtp_crawler.smtp_data
as
with conversations as (select
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