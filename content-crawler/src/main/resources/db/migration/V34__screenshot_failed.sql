UPDATE content_crawl_result
   SET screenshot_status = 'UnexpectedError'
 WHERE ok = FALSE
   AND screenshot_status IS NULL
   AND problem LIKE '%Protocol error (Page.captureScreenshot)%'