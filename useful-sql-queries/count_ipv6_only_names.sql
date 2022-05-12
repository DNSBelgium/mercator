-- Count amount of prefixes with AAAA records but no A records (per label)

SELECT coalesce(l.labels, 'no_label') AS label, domain_name
FROM dns_crawler.request req
    LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON l.visit_id = req.visit_id
    JOIN dns_crawler.response resp ON resp.request_id = req.id
GROUP BY domain_name, coalesce(l.labels, 'no_label')
HAVING SUM(CASE WHEN record_type = 'A' THEN 1 ELSE 0 END) = 0
   AND SUM(CASE WHEN record_type = 'AAAA' THEN 1 ELSE 0 END) > 0
