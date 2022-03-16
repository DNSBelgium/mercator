CREATE TABLE content_crawl_result
(
    id                      SERIAL          PRIMARY KEY,
    visit_id                UUID            NOT NULL,
    domain_name             VARCHAR(128)    NOT NULL,
    url                     VARCHAR(255)    NOT NULL,
    ok                      BOOLEAN         NOT NULL,
    problem                 TEXT,
    http_response_code      INTEGER,
    html_content_location   VARCHAR(255),
    screenshot_location     VARCHAR(255),
    har_location            VARCHAR(255),
    result_json             TEXT
);