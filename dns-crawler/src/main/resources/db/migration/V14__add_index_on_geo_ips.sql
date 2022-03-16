-- avoid full table scan when joining with dns_crawl_result
CREATE INDEX ON dns_crawl_result_geo_ips (dns_crawl_result_id);