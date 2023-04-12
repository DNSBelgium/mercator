
/* first add new column for html*/
ALTER TABLE content_crawler.content_crawl_result ADD html_status TEXT;

/* first add new column for screenshot*/
ALTER TABLE content_crawler.content_crawl_result ADD screenshot_status TEXT;

/*migrate values from ok to html*/
UPDATE content_crawler.content_crawl_result SET html_status = CASE
                                         WHEN ok = true THEN 'succeeded'
                                         WHEN ok = false THEN 'failed'
                                         ELSE 'unknown'
END;

/*migrate values from ok to screenshot*/
UPDATE content_crawler.content_crawl_result SET screenshot_status = CASE
                                         WHEN ok = true THEN 'succeeded'
                                         WHEN ok = false THEN 'failed'
                                         ELSE 'unknown'
END;


