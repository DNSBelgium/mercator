select l.labels, count(1) as count, count(distinct domain_name) as distinct_count
from dispatcher.dispatcher_event e
         join dispatcher.dispatcher_event_labels l on l.visit_id = e.visit_id
where l.labels in ('todo')
group by l.labels;