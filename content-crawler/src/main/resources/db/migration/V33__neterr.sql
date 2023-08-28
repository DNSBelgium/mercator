UPDATE content_crawl_result
SET html_status = 'UnexpectedError',
    screenshot_status = 'UnexpectedError'
WHERE html_status IS NULL
  AND screenshot_status IS NULL
  AND ok = FALSE
  AND (problem LIKE '["net::ERR_INVALID_REDIRECT at%'
   OR  problem LIKE '["net::ERR_SSL_KEY_USAGE_INCOMPATIBLE at %'
   OR  problem LIKE '["Protocol error (%'
   OR  problem LIKE '["net::ERR_INVALID_ARGUMENT %'
   OR  problem='["Navigation failed because browser has disconnected!"]')