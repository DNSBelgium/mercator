-- https://jira.office.dnsbelgium.be/jira/browse/PA-12498
-- due to a bug, some rows had wrong structure

update dns_crawler.dns_crawl_result
set all_records = '{}'::jsonb
where ok = False
  and all_records =
      '{"@": {"A": [], "MX": [], "NS": [], "SOA": [], "TXT": [], "AAAA": [], "DNSKEY": []}}'::jsonb
;


update dns_crawler.dns_crawl_result
set all_records = '{}'::jsonb
where ok = False
  and all_records = '{"@": {"A": [], "DS": [], "MX": [], "NS": [], "SOA": [], "SRV": [], "TXT": [], "AAAA": [], "CNAME": [], "DNSKEY": [], "CDNSKEY": []}}'::jsonb
;
