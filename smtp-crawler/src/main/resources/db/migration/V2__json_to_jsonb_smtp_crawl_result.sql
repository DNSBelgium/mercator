
ALTER TABLE smtp_crawler.smtp_crawl_result  ALTER COLUMN data SET DATA TYPE jsonb ;
ALTER TABLE smtp_crawler.smtp_crawl_result  rename COLUMN data to servers;
