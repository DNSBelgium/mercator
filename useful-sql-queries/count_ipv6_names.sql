-- Count all names that have at least one AAAA record

select l.labels, count(distinct domain_name) count
from dns_crawler.dns_crawl_result d
         join dispatcher.dispatcher_event_labels l on d.visit_id = l.visit_id
where labels in ('todo')
  and
    ( jsonb_array_length(all_records -> '@' -> 'records' -> 'AAAA')  +
    jsonb_array_length(all_records -> 'www' -> 'records' -> 'AAAA')   ) > 0
group by l.labels