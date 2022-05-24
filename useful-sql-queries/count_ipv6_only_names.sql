-- Count amount of prefixes with AAAA records but no A records (per label)

SELECT label, domain_name
FROM (
         SELECT coalesce(l.labels, 'no_label') AS label, domain_name, SUM(CASE WHEN record_type = 'A' THEN num_of_responses ELSE 0 END) number_a_record, SUM(CASE WHEN record_type = 'AAAA' THEN num_of_responses ELSE 0 END) number_aaaa_record
         FROM dns_crawler.request req
                  LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON l.visit_id = req.visit_id
         GROUP BY label, domain_name
     ) sub
WHERE number_a_record = 0
  AND number_aaaa_record > 0