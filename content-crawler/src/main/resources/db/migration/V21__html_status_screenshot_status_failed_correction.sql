UPDATE content_crawler.content_crawl_result SET html_status = CASE
                                                                  WHEN problem = '["Navigation timeout of 15000 ms exceeded"]' THEN 'Failed: Time out error'
                                                                  WHEN problem = '["net::ERR_NAME_NOT_RESOLVED at *]' THEN 'Failed: Name not resolved'
                                                                  WHEN problem = '["Protocol error (Emulation.setDeviceMetricsOverride): Target closed."]' THEN 'Failed: Unexpected error'
                                                                  ELSE 'unknown'
    END;

/*migrate values from ok to screenshot*/
UPDATE content_crawler.content_crawl_result SET screenshot_status = CASE
                                                                        WHEN problem = '["Navigation timeout of 15000 ms exceeded"]' THEN 'Failed: Time out error'
                                                                        WHEN problem = '["net::ERR_NAME_NOT_RESOLVED at *]' THEN 'Failed: Name not resolved'
                                                                        WHEN problem = '["Protocol error (Emulation.setDeviceMetricsOverride): Target closed."]' THEN 'Failed: Unexpected error'
                                                                        ELSE 'unknown'
END;