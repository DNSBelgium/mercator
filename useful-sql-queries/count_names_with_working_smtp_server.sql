-- count all names that have a working SMTP server

with smtp as
         (
             select visit_id
                  , domain_name
                  , jsonb_array_elements(jsonb_array_elements(servers) -> 'hosts') ->> 'connectOK' connectOK
             from smtp_crawler.smtp_crawl_result
         )
select l.labels, count(distinct domain_name) count
from smtp s
         join dispatcher.dispatcher_event_labels l on s.visit_id = l.visit_id
where l.labels in ('todo')
  and connectOK = 'true'
group by l.labels