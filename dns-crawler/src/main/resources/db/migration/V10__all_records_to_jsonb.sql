ALTER TABLE dns_crawl_result
    ALTER COLUMN all_records SET DATA TYPE jsonb USING all_records::jsonb;
