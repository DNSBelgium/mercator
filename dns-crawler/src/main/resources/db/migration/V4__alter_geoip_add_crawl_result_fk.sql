ALTER TABLE geo_ip ADD dns_crawl_result INTEGER;
UPDATE geo_ip g SET dns_crawl_result = (select r.id from dns_crawl_result r where r.crawl_id = g.crawl_id);
ALTER TABLE geo_ip ALTER COLUMN dns_crawl_result SET NOT NULL;
ALTER TABLE geo_ip ADD CONSTRAINT geo_ip_dns_crawl_result_id_fk FOREIGN KEY (dns_crawl_result) REFERENCES dns_crawl_result (id);
ALTER TABLE geo_ip DROP COLUMN crawl_id;
