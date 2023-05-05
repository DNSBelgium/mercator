
UPDATE content_crawler.content_crawl_result
SET html_status='NameNotResolved', screenshot_status='NameNotResolved'
WHERE ok = false
  AND html_status is NULL
  AND screenshot_status is NULL
  AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %';
