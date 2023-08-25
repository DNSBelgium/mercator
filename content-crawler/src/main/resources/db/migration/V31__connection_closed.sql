UPDATE content_crawl_result
   SET html_status = 'UnexpectedError',
           screenshot_status = 'UnexpectedError'
 WHERE html_status is null
   AND screenshot_status is null
   AND (problem LIKE '["net::ERR_CONNECTION_CLOSED%'
    OR  problem LIKE '["net::ERR_SSL_VERSION_OR_CIPHER_MISMATCH%'
    OR  problem LIKE '["Execution context was destroyed, most likely because of a navigation."%'
    OR  problem LIKE '["net::ERR_HTTP2_PROTOCOL_ERROR%'
    OR  problem LIKE '["net::ERR_BAD_SSL_CLIENT_AUTH_CERT%')
