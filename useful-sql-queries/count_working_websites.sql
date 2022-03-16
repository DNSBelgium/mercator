select l.labels, count(distinct domain_name) count
from content_crawler.content_crawl_result c
         join dispatcher.dispatcher_event_labels l on l.visit_id = c.visit_id
where l.labels in ('todo')
  and ok = True
group by l.labels

