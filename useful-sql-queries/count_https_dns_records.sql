-- Count amount of HTTPS records (per label)

SELECT l.labels, COUNT(DISTINCT r.domain_name) amount
FROM dns_crawler.request r JOIN dispatcher.dispatcher_event_labels l
    ON r.visit_id = l.visit_id
WHERE r.record_type = 'HTTPS'
--     AND l.labels IN ('some_label')
GROUP BY l.labels;

-- number of responses for certain record_type per visit_id
-- how many domains have how many HTTPS record responses
SELECT r.visit_id, COUNT(r2.*) amount
FROM dns_crawler.request r JOIN dns_crawler.response r2
    ON r.id = r2.request_id
WHERE r.record_type = 'HTTPS'
GROUP BY r.visit_id
ORDER BY amount DESC;

SELECT r.visit_id, COUNT(r.record_type = 'HTTPS') amount
FROM dns_crawler.request r JOIN dns_crawler.response r2
    ON r.id = r2.request_id
WHERE r.record_type = 'HTTPS'
GROUP BY r.visit_id
HAVING COUNT(r.record_type = 'HTTPS') > 1;

-- 103 or so duplicates?
SELECT r.*, r2.*
FROM dns_crawler.request r JOIN dns_crawler.response r2
    ON r.id = r2.request_id
WHERE visit_id = '2996d532-8b8c-66a0-35a0-b97d05acbba1';