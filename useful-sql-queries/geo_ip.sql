-- Select all IP information found by DNS Crawler per domain name (and label)

SELECT coalesce(l.labels, 'no_label'), r.domain_name, ip.ip, ip.asn, ip.asn_organisation, ip.country
FROM dns_crawler.request r
     LEFT OUTER JOIN dispatcher.dispatcher_event_labels l ON r.visit_id = l.visit_id
     JOIN dns_crawler.response r2 ON r.id = r2.request_id
     JOIN dns_crawler.response_geo_ips ip ON r2.id = ip.response_id
LIMIT 100;