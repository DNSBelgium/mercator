--delete from smtp_crawler.smtp_visit;

insert into smtp_visit(visit_id, domain_name, timestamp, num_conversations, crawl_status)
select
    d.visit_id, d.domain_name, d.timestamp, count(1)
     , case
           when crawl_status = 0 then 'OK'
           when crawl_status = 1 then 'MALFORMED_URL'
           when crawl_status = 2 then 'TIME_OUT'
           when crawl_status = 3 then 'UNKNOWN_HOST'
           when crawl_status = 4 then 'NETWORK_ERROR'
           when crawl_status = 9 then 'CONNECTION_REFUSED'
           when crawl_status = 10 then 'PROTOCOL_ERROR'
           when crawl_status = 32 then 'INVALID_HOSTNAME'
           when crawl_status = 15 then 'NO_IP_ADDRESS'
           when crawl_status = 99 then 'INTERNAL_ERROR'
    end as crawl_status
from smtp_data d
group by d.visit_id, d.domain_name, d.timestamp, d.crawl_status
;
---
insert into smtp_visit(visit_id, domain_name, timestamp, num_conversations, crawl_status)
select
    d.visit_id, d.domain_name, d.crawl_timestamp, 0 as num_conversations
     , case
         when crawl_status = 0 then 'OK'
         when crawl_status = 1 then 'MALFORMED_URL'
         when crawl_status = 2 then 'TIME_OUT'
         when crawl_status = 3 then 'UNKNOWN_HOST'
         when crawl_status = 4 then 'NETWORK_ERROR'
         when crawl_status = 9 then 'CONNECTION_REFUSED'
         when crawl_status = 10 then 'PROTOCOL_ERROR'
         when crawl_status = 32 then 'INVALID_HOSTNAME'
         when crawl_status = 15 then 'NO_IP_ADDRESS'
         when crawl_status = 99 then 'INTERNAL_ERROR'
       end as crawl_status
from smtp_crawler.smtp_crawl_result d
where jsonb_array_length(servers) = 0
group by d.visit_id, d.domain_name, d.crawl_timestamp, d.crawl_status
;