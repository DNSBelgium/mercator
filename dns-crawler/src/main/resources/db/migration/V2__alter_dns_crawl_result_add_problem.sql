ALTER TABLE dns_crawl_result RENAME domain_exists TO ok;
ALTER TABLE dns_crawl_result ADD problem TEXT;