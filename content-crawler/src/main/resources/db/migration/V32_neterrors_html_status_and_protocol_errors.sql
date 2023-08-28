UPDATE content_crawl_result
   SET html_status = 'UnexpectedError',
       screenshot_status = 'UnexpectedError'
 WHERE html_status IS NULL
   AND screenshot_status IS NULL
   AND ok = FALSE
   AND (problem LIKE '["net::ERR_INVALID_RESPONSE at%'
    OR problem LIKE '["net::ERR_ABORTED at %'
    OR problem LIKE '["net::ERR_SSL_SERVER_CERT_BAD_FORMAT at %'
    OR problem LIKE '["net::ERR_UNSAFE_REDIRECT at %'
    OR problem LIKE '["Protocol error (Page.navigate):%')
