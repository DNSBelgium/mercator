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
         , min(timestamp)  over (partition by ip, day) as min_timestamp_per_ip_per_day
    from per_day pd
)
select *
from data
;