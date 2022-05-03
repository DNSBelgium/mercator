-- New table request will take over dns_crawl_result.
CREATE TABLE dns_crawler.request
(
    id              SERIAL PRIMARY KEY,
    visit_id        UUID                     NOT NULL,
    domain_name     VARCHAR(128)             NOT NULL, -- abc.be
    prefix          VARCHAR(63)              NOT NULL, -- @, www, _dmarc
    record_type     CHAR(10)                 NOT NULL, -- A, AAAA, NS, ...
    rcode           INT,                               -- Success code for the requested record.
    crawl_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    ok              BOOLEAN,
    problem         TEXT
);

-- New table response will take over dns_crawl_result's Json data.
CREATE TABLE dns_crawler.response
(
    id          SERIAL PRIMARY KEY,
    record_data TEXT   NOT NULL, -- 94.126.48.90, s1.named.be., ...
    ttl         INT,             -- time-to-live
    request_id  INT
);

ALTER TABLE dns_crawler.response -- Adding foreign key constraint.
    ADD CONSTRAINT dns_response_request_id_fk FOREIGN KEY (request_id) REFERENCES dns_crawler.request(id);

-- Adjusting dns_crawl_result_geo_ips
ALTER TABLE dns_crawler.dns_crawl_result_geo_ips
    RENAME TO response_geo_ips;

-- Adding a response_id value to geo ip first.
-- After data transfer is complete, dns_crawl_result_id (which is currently the FK) will be removed.
-- Then response_id will be altered to be the new FK.
ALTER TABLE dns_crawler.response_geo_ips
    ADD COLUMN response_id INT;

-- Adding ip_version to response_geo_ips to remove record_type later. (record_type is currently A / AAAA in stead of ip_version 4 / 6)
ALTER TABLE dns_crawler.response_geo_ips
    ADD COLUMN ip_version INT; -- A == 4, AAAA == 6
