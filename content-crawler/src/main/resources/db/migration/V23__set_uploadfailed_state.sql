UPDATE content_crawl_result
SET html_status = 'UploadFailed'
WHERE ok = false
  AND html_status IS NULL
  AND problem LIKE '%Upload failed for html %';

UPDATE content_crawl_result
SET screenshot_status = 'UploadFailed'
WHERE ok = false
  AND screenshot_status IS NULL
  AND problem LIKE '%Upload failed for screenshot %';
