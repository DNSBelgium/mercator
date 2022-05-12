-- Count amount of AAAA records (per label)

SELECT coalesce(l.labels, 'no_label'), domain_name, count(*) AS number_of_records
FROM dns_crawler.request req
    LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON l.visit_id = req.visit_id
    JOIN dns_crawler.response resp ON resp.request_id = req.id
WHERE req.record_type = 'AAAA'
GROUP BY domain_name, coalesce(l.labels, 'no_label')