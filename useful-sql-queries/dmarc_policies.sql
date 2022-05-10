-- Get count of NOT NULL _dmarc prefixes to see how have a record_data containing 'none', 'quarantine' or 'reject'.

WITH groups AS (
    SELECT CASE
        WHEN (req.prefix = '_dmarc' AND res.record_data ~ 'v=DMARC1') THEN
            CASE
                WHEN (res.record_data ~ 'p=none') THEN 'none'
                WHEN (res.record_data ~ 'p=quarantine') THEN 'quarantine'
                WHEN (res.record_data ~ 'p=reject') THEN 'reject'
                ELSE 'no_policy'
                END
            ELSE 'not_dmarc'
            END p,
        l.labels
    FROM dns_crawler.request req JOIN dns_crawler.response res
        ON req.id = res.request_id
        JOIN dispatcher.dispatcher_event_labels l
            ON req.visit_id = l.visit_id
    WHERE req.record_type = 'TXT' AND req.visit_id = l.visit_id
)
SELECT p, COUNT(*) amount
FROM groups
GROUP BY p;