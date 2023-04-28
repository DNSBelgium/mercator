/* first add new column for html*/
ALTER TABLE content_crawler.content_crawl_result
    ADD html_status TEXT;

/* first add new column for screenshot*/
ALTER TABLE content_crawler.content_crawl_result
    ADD screenshot_status TEXT;

/*html status*/
UPDATE content_crawler.content_crawl_result
SET html_status = 'Ok'
WHERE ok = true
  AND html_status != 'HtmlTooBig';

UPDATE content_crawler.content_crawl_result
SET html_status = 'HtmlTooBig'
WHERE ok = true
  AND html_status = 'HtmlTooBig';

UPDATE content_crawler.content_crawl_result
SET html_status = 'TimeOut'
WHERE ok = false
  AND problem LIKE '["Navigation timeout %';

UPDATE content_crawler.content_crawl_result
SET html_status = 'NameNotResolved'
WHERE ok = false
  AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %';

UPDATE content_crawler.content_crawl_result
SET html_status = 'UploadFailed'
WHERE ok = false
  AND problem LIKE '["Upload failed for html %';

UPDATE content_crawler.content_crawl_result
SET html_status = 'UnexpectedError'
WHERE ok = false
  AND problem NOT LIKE '["Navigation timeout %'
  AND problem NOT LIKE '["Upload failed for html %'
  AND problem NOT LIKE '["net::ERR_NAME_NOT_RESOLVED at %';

/*screenshot status*/
UPDATE content_crawler.content_crawl_result
SET screenshot_status = 'Ok'
WHERE ok = true
  AND screenshot_status != 'ScreenshotTooBig';

UPDATE content_crawler.content_crawl_result
SET screenshot_status = 'ScreenshotTooBig'
WHERE ok = true
  AND screenshot_status = 'ScreenshotTooBig';

UPDATE content_crawler.content_crawl_result
SET screenshot_status = 'TimeOut'
WHERE ok = false
  AND problem LIKE '["Navigation timeout %';

UPDATE content_crawler.content_crawl_result
SET screenshot_status = 'NameNotResolved'
WHERE ok = false
  AND problem LIKE '["net::ERR_NAME_NOT_RESOLVED at %';

UPDATE content_crawler.content_crawl_result
SET screenshot_status = 'UploadFailed'
WHERE ok = false
  AND problem LIKE '["Upload failed for html %';

UPDATE content_crawler.content_crawl_result
SET screenshot_status = 'UnexpectedError'
WHERE ok = false
  AND problem NOT LIKE '["Navigation timeout %'
  AND problem NOT LIKE '["Upload failed for screenshot %'
  AND problem NOT LIKE '["net::ERR_NAME_NOT_RESOLVED at %';
