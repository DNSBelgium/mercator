-- Count amount of domain_names have HTTPS records per label.

SELECT l.labels, COUNT(1), COUNT(DISTINCT domain_name)
FROM dns_crawler.response resp
    JOIN dns_crawler.request req ON resp.request_id = req.id
    JOIN dispatcher.dispatcher_event_labels l ON req.visit_id = l.visit_id
WHERE record_type = 'HTTPS'
GROUP BY l.labels;