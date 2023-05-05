/* first add new column for html*/
ALTER TABLE content_crawl_result
    ADD html_status TEXT;

/* first add new column for screenshot*/
ALTER TABLE content_crawl_result
    ADD screenshot_status TEXT;

-- existing records will be updated in following migrations