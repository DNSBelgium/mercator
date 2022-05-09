-- Count amount of AAAA records (per label)

SELECT l.labels, COUNT(DISTINCT r.domain_name) amount
FROM dns_crawler.request r JOIN dispatcher.dispatcher_event_labels l
    ON r.visit_id = l.visit_id
WHERE r.record_type = 'AAAA'
--     AND l.labels IN ('some_label')
GROUP BY l.labels;