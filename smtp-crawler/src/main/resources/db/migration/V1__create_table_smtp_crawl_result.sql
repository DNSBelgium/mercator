CREATE TABLE smtp_crawl_result
(
    id              SERIAL PRIMARY KEY,
    visit_id        UUID                        NOT NULL,
    crawl_timestamp timestamp with time zone    NOT NULL,
    domain_name     VARCHAR(128)                NOT NULL,
    ok              BOOLEAN                     NOT NULL,
    data            JSON
);