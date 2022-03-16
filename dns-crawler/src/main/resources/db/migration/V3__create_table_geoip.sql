CREATE TABLE geo_ip
(
    id          SERIAL PRIMARY KEY,
    crawl_id    UUID         NOT NULL,
    record_type VARCHAR(128) NOT NULL,
    ip          VARCHAR(128) NOT NULL,
    country     VARCHAR(128) NOT NULL,
    asn         VARCHAR(128) NOT NULL
);