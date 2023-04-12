CREATE TABLE smtp_server_host
(
    id        SERIAL PRIMARY KEY,
    host_id   int NOT NULL REFERENCES smtp_crawler.smtp_host,
    server_id int NOT NULL REFERENCES smtp_crawler.smtp_server
)