UPDATE content_crawler.content_crawl_result
SET problem = NULL
WHERE html_status != 'Failed: Unexpected error' AND screenshot_status != 'Failed: Unexpected error';

