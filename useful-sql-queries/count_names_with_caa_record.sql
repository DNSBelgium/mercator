-- count all names that have a CAA record

select l.labels, count(distinct domain_name) count
from dns_crawler.dns_crawl_result d
         join dispatcher.dispatcher_event_labels l on d.visit_id = l.visit_id
where l.labels in 'todo'
  and jsonb_array_length(all_records -> '@' -> 'records' -> 'CAA') > 0
group by l.labels
