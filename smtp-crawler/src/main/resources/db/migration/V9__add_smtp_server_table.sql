CREATE TABLE smtp_server
(
    id           SERIAL PRIMARY KEY,
    host_name    VARCHAR(128) NOT NULL,
    priority     int          NOT NULL,
    crawl_result int          NOT NULL REFERENCES smtp_crawl_result
);