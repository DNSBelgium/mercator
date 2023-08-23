UPDATE content_crawl_result
SET html_status = 'UploadFailed'
WHERE ok = FALSE
  AND problem ~ '(.+,|\[)"Upload failed for file \[(.+\/)index\.html] :.*'
  AND html_key is null
  AND html_status is null;

UPDATE content_crawl_result
set screenshot_status = 'UploadFailed'
WHERE ok = FALSE
  AND (problem ~ '(.+,|\[)"Upload failed for file \[(.+\/)screenshot\.(png|webp)] :.*')
  AND screenshot_key is null
  AND screenshot_status is null;

-- Similar for har_status in the future
-- problem ~ '(?:,|\[)"Upload failed for file \[(.+\/).+\.har] :'
