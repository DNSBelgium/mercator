-- Count amount of prefixes with AAAA records but no A records (per label)

SELECT l.labels, COUNT(DISTINCT r.domain_name) amount
FROM dns_crawler.request r JOIN dispatcher.dispatcher_event_labels l
    ON r.visit_id = l.visit_id
WHERE r.record_type = 'AAAA'
    AND r.prefix NOT IN (
        SELECT DISTINCT r2.prefix
        FROM dns_crawler.request r2
        WHERE r2.record_type = 'A'
    )
--     AND l.labels IN ('some_label')
GROUP BY l.labels, r.domain_name;