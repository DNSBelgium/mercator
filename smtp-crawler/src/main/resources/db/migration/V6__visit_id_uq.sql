DELETE FROM
    smtp_crawl_result a
    USING smtp_crawl_result b
WHERE a.id < b.id AND a.visit_id = b.visit_id;


alter table smtp_crawl_result
    add constraint smtp_crawl_result_visitid_uq unique (visit_id);
