-- Get count of NOT NULL _dmarc prefixes to see how have a record_data containing 'none', 'quarantine' or 'reject'.

SELECT req.domain_name,
       CASE
           WHEN (req.prefix = '_dmarc' AND resp.record_data ~ 'v=DMARC1') THEN
               CASE
                   WHEN (resp.record_data ~ 'p=none') THEN 'none'
                   WHEN (resp.record_data ~ 'p=quarantine') THEN 'quarantine'
                   WHEN (resp.record_data ~ 'p=reject') THEN 'reject'
                   ELSE 'no_policy'
                   END
           ELSE 'not_dmarc'
           END p
FROM dns_crawler.request req
         JOIN dns_crawler.response resp ON resp.request_id = req.id
WHERE req.record_type = 'TXT'
  AND req.prefix = '_dmarc'
  AND resp.record_data ~ 'v=DMARC1'