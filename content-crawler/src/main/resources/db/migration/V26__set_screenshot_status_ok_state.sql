UPDATE content_crawl_result SET screenshot_status='Ok'
WHERE screenshot_status IS NULL
  AND ok=TRUE
  AND problem IS NULL
  AND screenshot_key IS NOT NULL
