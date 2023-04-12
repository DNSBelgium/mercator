CREATE TABLE crawl_result_server
(
    id        SERIAL PRIMARY KEY,
    crawl_id  int NOT NULL REFERENCES smtp_crawler.smtp_crawl_result,
    server_id int NOT NULL REFERENCES smtp_crawler.smtp_server
)