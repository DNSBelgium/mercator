-- Count amount of AAAA records (per label)

SELECT coalesce(l.labels, 'no_label'), prefix, domain_name, num_of_responses
FROM dns_crawler.request req
         LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON l.visit_id = req.visit_id
WHERE req.record_type = 'AAAA'