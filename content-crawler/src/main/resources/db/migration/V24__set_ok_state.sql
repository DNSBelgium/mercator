UPDATE content_crawl_result
   SET html_status='Ok',
       screenshot_status='Ok'
 WHERE ok=true
   AND problem is null
   AND html_status is null
   AND screenshot_status is null
   AND html_key is not null;
