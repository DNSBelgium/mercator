ALTER TABLE content_crawl_result RENAME html_content_location TO html_location;
ALTER TABLE content_crawl_result RENAME result_json TO metrics_json;
ALTER TABLE content_crawl_result ADD final_url VARCHAR(255);
ALTER TABLE content_crawl_result ADD html_length INTEGER;
ALTER TABLE content_crawl_result DROP http_response_code;