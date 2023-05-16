drop table smtp_crawler.per_ip_per_day;

create table smtp_crawler.per_ip_per_day
as
with per_day
         as (select
                 s.*
                  , date_trunc('day', timestamp) as day
             from smtp_crawler.smtp_data s
    )
        , data as (
    select pd.*
         , row_number() over (partition by ip, day order by timestamp) as rownr
    from per_day pd
) ,
     d2 as (select *
            from data
            where rownr = 1
     )
select
      visit_id
    , domain_name
    , hostname
    , priority
    , ip
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
from d2
;

drop table smtp_crawler.not_per_ip_per_day;

create table smtp_crawler.not_per_ip_per_day
as
with per_day
         as (select
                 s.*
                  , date_trunc('day', timestamp) as day
             from smtp_crawler.smtp_data s
    )
        , data as (
    select pd.*
         , row_number() over (partition by ip, day order by timestamp) as rownr
         , min(timestamp)  over (partition by ip, day) as min_timestamp_per_ip_per_day
    from per_day pd
)
select *
from data
;