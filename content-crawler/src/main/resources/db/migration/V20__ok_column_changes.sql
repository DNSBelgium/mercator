/*
alter table content_crawler.content_crawl_result
    rename column ok to crawl_status;

alter table content_crawler.content_crawl_result
alter column crawl_status type text using crawl_status::text;

UPDATE content_crawler.content_crawl_result
SET crawl_status = CASE
                       WHEN crawl_status = true THEN 'succeeded'
                       WHEN crawl_status = false THEN 'Failed: Unexpected error'
END;
*/
/*
/* first add new table */
ALTER TABLE content_crawler.content_crawl_result ADD crawl_status VARCHAR(10);

/*migrate values fron ok to new column*/
UPDATE content_crawler.content_crawl_result SET crawl_status = CASE
                                         WHEN ok = true THEN 'succeeded'
                                         WHEN ok = false THEN 'failed'
                                         ELSE 'unknown'
END;
/*delete ok column later if this succeds*/
*/