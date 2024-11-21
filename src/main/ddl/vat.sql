create table vat_crawler.vat_crawl_result
(
    id             bigint       primary key,
    visit_id       varcha(26),
    domain_name    varchar(255),
    start_url      varchar(255),
    matching_url   varchar(255),
    crawl_started  timestamp with time zone,
    crawl_finished timestamp with time zone,
    visited_urls   varchar[]
);

create table vat_crawler.page_visit
(
    id             bigint     primary key,
    visit_id       varchar,
    domain_name    varchar(255),
    crawl_started  timestamp with time zone,
    crawl_finished timestamp with time zone,
    body_text      text,
    status_code    integer,
    url            varchar(500),
    path           varchar(500),
    vat_values     jsonb,
    link_text      varchar(500)
);
