CREATE TABLE dns_crawl_result_geo_ips
(
    dns_crawl_result_id INTEGER NOT NULL,
    asn                 VARCHAR(255),
    country             VARCHAR(255),
    ip                  VARCHAR(255),
    record_type         VARCHAR(128)
);
DROP TABLE geo_ip;

ALTER TABLE dns_crawl_result_geo_ips ADD CONSTRAINT geo_ip_dns_crawl_result_id_fk FOREIGN KEY (dns_crawl_result_id) REFERENCES dns_crawl_result (id);
