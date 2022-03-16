-- https://jira.office.dnsbelgium.be/jira/browse/PA-12498
-- due to a bug, some rows had wrong structure

update dns_crawl_result
set all_records = jsonb_build_object('@', json_build_object('records', all_records -> '@'))
where all_records -> '@' -> 'A' is not null
  and all_records -> '@' -> 'records' is null
  and ok = True;
