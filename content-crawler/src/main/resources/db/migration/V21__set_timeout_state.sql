UPDATE content_crawl_result
SET html_status = 'TimeOut', screenshot_status = 'TimeOut'
WHERE html_status IS NULL
  AND screenshot_status IS NULL
  AND ok = false
  AND problem LIKE '["Navigation timeout %';
