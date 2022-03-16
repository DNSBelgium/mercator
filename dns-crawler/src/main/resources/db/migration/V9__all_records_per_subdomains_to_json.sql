UPDATE dns_crawl_result
    SET all_records = jsonb_insert('{}', '{"@"}', all_records :: jsonb #> '{records}');

ALTER TABLE dns_crawl_result
    ALTER COLUMN all_records SET DATA TYPE json USING all_records::json;
