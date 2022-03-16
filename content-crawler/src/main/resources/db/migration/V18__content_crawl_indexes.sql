create index content_crawl_visit_id_url on content_crawl_result(visit_id, url);
create index content_crawl_domain_name  on content_crawl_result(domain_name);
create index content_crawl_timestamp    on content_crawl_result(crawl_timestamp);
