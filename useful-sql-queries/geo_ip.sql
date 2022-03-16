-- Find the IP addresses found by the DNS crawler and the corresponding geo info
select domain_name
     , g.ip
     , g.asn
     , g.asn_organisation
     , g.country
from dns_crawler.dns_crawl_result r
     join dns_crawler.dns_crawl_result_geo_ips g on r.id = g.dns_crawl_result_id
limit 100

