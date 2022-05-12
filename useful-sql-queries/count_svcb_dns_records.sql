-- Count amount of SVCB records (per label)

SELECT coalesce(l.labels, 'no_label'), domain_name, count(*) AS number_of_records
FROM dns_crawler.request req
    JOIN dns_crawler.response resp ON resp.request_id = req.id
    LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON l.visit_id = req.visit_id
WHERE req.record_type = 'SVCB'
GROUP BY domain_name, coalesce(l.labels, 'no_label')