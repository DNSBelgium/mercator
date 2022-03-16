CREATE TABLE dns_crawl_result
(
    id            SERIAL PRIMARY KEY,
    crawl_id      UUID         NOT NULL,
    domain_exists BOOLEAN      NOT NULL,
    domain_name   VARCHAR(128) NOT NULL,
    all_records   TEXT         NOT NULL
);