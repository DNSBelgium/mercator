

CREATE TABLE technology_crawler.technology_crawl_result (
    id             BIGINT       PRIMARY KEY,
    visit_id       VARCHAR(26),
    domain_name    VARCHAR(255),
    start_url      VARCHAR(255),
    matching_url   VARCHAR(255),
    crawl_started  TIMESTAMP WITH TIME ZONE,
    crawl_finished TIMESTAMP WITH TIME ZONE,
    visited_urls   VARCHAR[],
    detected_technologies VARCHAR[]
);