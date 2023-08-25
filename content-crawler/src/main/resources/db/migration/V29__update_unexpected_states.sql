UPDATE content_crawl_result
SET html_status = 'UnexpectedError',
    screenshot_status = 'UnexpectedError'
WHERE html_status is null
  AND screenshot_status is null
  AND (problem LIKE '["net::ERR_ADDRESS_UNREACHABLE%'
   OR problem LIKE '["net::ERR_CONNECTION_RESET%'
   OR problem LIKE '["net::ERR_EMPTY_RESPONSE%'
   OR problem LIKE '["net::ERR_TOO_MANY_REDIRECTS%'
   OR problem LIKE '["net::ERR_CONNECTION_REFUSED%')
