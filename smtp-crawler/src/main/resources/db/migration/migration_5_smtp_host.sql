--delete from smtp_crawler.smtp_host;

insert into smtp_host(visit_id, from_mx, host_name, priority, conversation)
select
    d.visit_id
     ,    case
              when (d.priority > 0) then true
              when (d.hostname != d.domain_name) then true
              else false
    end as from_mx
     , d.hostname
     , d.priority
     , c.id as conversation
from per_ip_per_day d
    join smtp_conversation c on c.ip = d.ip and d.min_timestamp_per_ip_per_day = c.timestamp
;