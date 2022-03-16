ALTER TABLE content_crawler.content_crawl_result ADD bucket VARCHAR(255);
ALTER TABLE content_crawler.content_crawl_result ADD html_key VARCHAR(255);
ALTER TABLE content_crawler.content_crawl_result ADD screenshot_key VARCHAR(255);
ALTER TABLE content_crawler.content_crawl_result ADD har_key VARCHAR(255);

UPDATE content_crawler.content_crawl_result
SET
    bucket = regexp_replace(html_location, '(https:\/\/)(dc3-eu-west-1-.*-mercator-muppets)(\..*)', '\2'),
    html_key = regexp_replace(html_location, '(https:\/\/dc3-eu-west-1-.*-mercator-muppets[^\/]*)\/(.*$)', '\2'),
    screenshot_key = regexp_replace(screenshot_location, '(https:\/\/dc3-eu-west-1-.*-mercator-muppets[^\/]*)\/(.*$)', '\2'),
    har_key = regexp_replace(har_location, '(https:\/\/dc3-eu-west-1-.*-mercator-muppets[^\/]*)\/(.*$)', '\2');
