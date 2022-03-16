-- count all websites where wappalyzer found a cryptominer

select l.labels, count(1) count
from content_crawler.technologies_with_category t
         join dispatcher.dispatcher_event_labels l on l.visit_id = t.visit_id
where l.labels in ('todo')
  and category_slug = 'cryptominers'
group by l.labels
