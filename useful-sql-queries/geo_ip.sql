-- Select all IP information found by DNS Crawler per domain name (and label)

SELECT l.labels, r.domain_name, ip.*
FROM dns_crawler.request r JOIN dispatcher.dispatcher_event_labels l
    ON r.visit_id = l.visit_id
    JOIN dns_crawler.response r2
        ON r.id = r2.request_id
        JOIN dns_crawler.response_geo_ips ip
            ON r2.id = ip.response_id
-- WHERE l.labels IN ('some_label')
LIMIT 100;