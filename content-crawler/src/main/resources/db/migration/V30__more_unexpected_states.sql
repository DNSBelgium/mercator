UPDATE content_crawl_result
SET html_status = 'UnexpectedError',
    screenshot_status = 'UnexpectedError'
WHERE html_status is null
  AND screenshot_status is null
  AND (problem LIKE '["net::ERR_SSL_PROTOCOL_ERROR%'
   OR problem LIKE '["Protocol error (Page.captureScreenshot): %'
   OR problem LIKE '["net::ERR_SSL_UNRECOGNIZED_NAME_ALERT%')