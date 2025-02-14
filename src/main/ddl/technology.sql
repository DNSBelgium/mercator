

CREATE TABLE technology_crawler.technology_crawl_result (
    id             BIGINT       PRIMARY KEY,
    visit_id       VARCHAR(26),
    domain_name    VARCHAR(255),
    detected_technologies VARCHAR[]
);