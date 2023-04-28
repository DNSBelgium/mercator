UPDATE content_crawler.content_crawl_result SET html_status = CASE
                                                                  WHEN ok = true AND html_status != 'Failed: Html file too big' THEN 'succeeded'
                                                                  WHEN ok = true AND html_status = 'Failed: Html file too big'
                                                                      THEN 'Failed: Html file too big'
                                                                  WHEN ok = false AND problem LIKE '["Navigation timeout %'
                                                                      THEN 'Failed: Time out error'
                                                                  WHEN ok = false AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %'
                                                                      THEN 'Failed: Name not resolved'
                                                                  WHEN ok = false AND problem LIKE '["Upload failed for html %'
                                                                      THEN 'Failed: Upload failed'
                                                                  ELSE 'Failed: Unexpected error'
    END;

UPDATE content_crawler.content_crawl_result SET screenshot_status = CASE
                                                                        WHEN ok = true AND screenshot_status != 'Failed: Screenshot file too big' THEN 'succeeded'
                                                                        WHEN ok = true AND screenshot_status = 'Failed: Screenshot file too big'
                                                                            THEN 'Failed: Screenshot file too big'
                                                                        WHEN ok = false AND problem LIKE '["Navigation timeout %'
                                                                            THEN 'Failed: Time out error'
                                                                        WHEN ok = false AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %'
                                                                            THEN 'Failed: Name not resolved'
                                                                        WHEN ok = false AND problem LIKE '["Upload failed for screenshot %'
                                                                            THEN 'Failed: Upload failed'
                                                                        ELSE 'Failed: Unexpected error'
    END;