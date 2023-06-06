UPDATE content_crawl_result SET html_status='Ok'
WHERE html_status IS NULL
  AND ok=TRUE
  AND problem IS NULL
  AND html_key IS NOT NULL;
