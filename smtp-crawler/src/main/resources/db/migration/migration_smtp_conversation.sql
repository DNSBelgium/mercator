delete from smtp_crawler.smtp_conversation;

insert into smtp_crawler.smtp_conversation
( ip
, asn
, country
, asn_organisation
, banner
, connect_ok
, connect_reply_code
, ip_version
, start_tls_ok
, start_tls_reply_code
, error_message
, connection_time_ms
, software
, software_version
, timestamp
, extensions)
select
    ip
     , asn
     , country
     , asn_organisation
     , banner
     , connect_ok
     , connect_reply_code
     , ip_version
     , start_tls_ok
     , start_tls_reply_code
     , error_message
     , connection_time_ms
     , software
     , software_version
     , timestamp
     , extensions
from smtp_crawler.per_ip_per_day
;