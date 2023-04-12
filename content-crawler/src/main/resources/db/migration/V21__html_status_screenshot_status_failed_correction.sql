UPDATE content_crawler.content_crawl_result SET html_status = CASE
                                                                  WHEN ok = true THEN 'succeeded'
                                                                  WHEN ok = false AND problem = '["Navigation timeout of 15000 ms exceeded"]' THEN 'Failed: Time out error'
                                                                  WHEN ok = false AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %' THEN 'Failed: Name not resolved'
                                                                  WHEN ok = false AND problem = '["Protocol error (Emulation.setDeviceMetricsOverride): Target closed."]' THEN 'Failed: Unexpected error'
                                                                  ELSE 'Failed: Unexpected error'
    END;

UPDATE content_crawler.content_crawl_result SET screenshot_status = CASE
                                                                        WHEN ok = true THEN 'succeeded'
                                                                        WHEN ok = false AND problem = '["Navigation timeout of 15000 ms exceeded"]' THEN 'Failed: Time out error'
                                                                        WHEN ok = false AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %' THEN 'Failed: Name not resolved'
                                                                        WHEN ok = false AND problem = '["Protocol error (Emulation.setDeviceMetricsOverride): Target closed."]' THEN 'Failed: Unexpected error'
                                                                        ELSE 'Failed: Unexpected error'
    END;