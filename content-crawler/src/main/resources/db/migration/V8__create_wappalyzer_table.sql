CREATE TABLE wappalyzer_result
(
    visit_id                UUID            PRIMARY KEY,
    domain_name             VARCHAR(128)    NOT NULL,
    url                     VARCHAR(255)    NOT NULL,
    ok                      BOOLEAN         NOT NULL,
    technologies            JSONB
);
