UPDATE content_crawl_result SET html_status='HtmlTooBig'
WHERE html_status IS NULL
  AND ok=TRUE
  AND problem IS NULL
  AND html_length IS NULL
  AND html_key IS NULL;

UPDATE content_crawl_result SET screenshot_status='HtmlTooBig'
WHERE screenshot_status IS NULL
  AND ok=TRUE
  AND problem IS NULL
  AND screenshot_key IS NULL;
