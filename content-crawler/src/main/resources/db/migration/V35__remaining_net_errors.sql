UPDATE content_crawl_result
   SET html_status='UnexpectedError'
 WHERE html_status IS NULL
   AND html_key IS NULL
   AND (problem LIKE '["net::ERR_ADDRESS_INVALID at %'
    OR  problem LIKE '["net::ERR_SOCKET_NOT_CONNECTED at %'
    OR  problem LIKE '["net::ERR_RESPONSE_HEADERS_TRUNCATED at %'
    OR  problem = '["Protocol error: Connection closed. Most likely the page has been closed."]'
    OR  problem LIKE '["net::ERR_INVALID_HTTP_RESPONSE at %'
    OR  problem LIKE '["net::ERR_CONNECTION_TIMED_OUT at %'
    OR  problem LIKE '["net::ERR_SSL_BAD_RECORD_MAC_ALERT at %'
    OR  problem LIKE '["net::ERR_TIMED_OUT at %'
    OR  problem LIKE '["net::ERR_HTTP2_SERVER_REFUSED_STREAM at %'
    OR  problem LIKE '["net::ERR_FAILED at %'
    OR  problem LIKE '["net::ERR_UNEXPECTED_PROXY_AUTH at %'
    OR  problem LIKE '["net::ERR_RESPONSE_HEADERS_MULTIPLE_LOCATION at %')
