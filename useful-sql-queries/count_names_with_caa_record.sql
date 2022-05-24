-- Count amount of CAA records (per label)

SELECT coalesce(l.labels, 'no_label'), domain_name, count(*) AS number_of_records
FROM dns_crawler.request req
    LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON l.visit_id = req.visit_id
WHERE req.record_type = 'CAA' and req.num_of_responses > 0
GROUP BY domain_name, coalesce(l.labels, 'no_label')