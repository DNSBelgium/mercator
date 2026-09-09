-- ---- helpers ----------------------------------------------------------------
create or replace macro pq(side, ds) as table
  select * from read_parquet(printf('/tmp/mercator-e2e/%s/data/%s/**/*.parquet', side, ds));

-- stable, semantic projections (exclude all timestamps / durations / TTL / geo)
create or replace macro dns_req(side) as table
  with a as (select visit_id, domain_name, unnest(requests) as r from pq(side,'dns'))
  select visit_id, domain_name, r.prefix, r.record_type, r.rcode, r.ok, r.num_of_responses from a;

create or replace macro dns_resp(side) as table
  with a as (select visit_id, domain_name, unnest(requests) as r from pq(side,'dns')),
       b as (select visit_id, domain_name, r.prefix prefix, r.record_type record_type, unnest(r.responses) as resp from a)
  select visit_id, domain_name, prefix, record_type, resp.record_data from b;

create or replace macro web_pv(side) as table
  with a as (select visit_id, domain_name, matching_url, vat_values, visited_urls, unnest(page_visits) as pv from pq(side,'web'))
  select visit_id, domain_name, matching_url, vat_values, visited_urls,
         pv.url, pv.final_url, pv.status_code, pv.path, pv.content_length,
         pv.detected_technologies, pv.vat_values as pv_vat from a;

create or replace macro wrb(side) as table
  select visit_id, domain_name, url, final_url, response_body from pq(side,'web_response_body');

create or replace macro tls_v(side) as table
  with a as (select visit_id, domain_name, unnest(visits) as v from pq(side,'tls'))
  select visit_id, domain_name, v.host_name, v.host_name_matches_certificate,
         v.chain_trusted_by_java_platform, v.certificate_expired, v.certificate_too_soon,
         v.certificate_chain_fingerprints,
         v.full_scan_entity.connect_ok, v.full_scan_entity.support_tls_1_3, v.full_scan_entity.support_tls_1_2,
         v.full_scan_entity.support_tls_1_1, v.full_scan_entity.support_tls_1_0,
         v.full_scan_entity.support_ssl_3_0, v.full_scan_entity.support_ssl_2_0,
         v.full_scan_entity.selected_cipher_tls_1_3, v.full_scan_entity.selected_cipher_tls_1_2,
         v.full_scan_entity.lowest_version_supported, v.full_scan_entity.highest_version_supported,
         v.full_scan_entity.server_name from a;

.mode box
.headers on

-- ---- 1) SCHEMA PARITY (expect 0 differences both ways) -----------------------
select 'dns' dataset,
  (select count(*) from ((select column_name,column_type from (describe select * from pq('batch','dns')))
                         except (select column_name,column_type from (describe select * from pq('pipeline','dns'))))) batch_only,
  (select count(*) from ((select column_name,column_type from (describe select * from pq('pipeline','dns')))
                         except (select column_name,column_type from (describe select * from pq('batch','dns'))))) pipe_only
union all select 'web',
  (select count(*) from ((select column_name,column_type from (describe select * from pq('batch','web')))
                         except (select column_name,column_type from (describe select * from pq('pipeline','web'))))),
  (select count(*) from ((select column_name,column_type from (describe select * from pq('pipeline','web')))
                         except (select column_name,column_type from (describe select * from pq('batch','web')))))
union all select 'web_response_body',
  (select count(*) from ((select column_name,column_type from (describe select * from pq('batch','web_response_body')))
                         except (select column_name,column_type from (describe select * from pq('pipeline','web_response_body'))))),
  (select count(*) from ((select column_name,column_type from (describe select * from pq('pipeline','web_response_body')))
                         except (select column_name,column_type from (describe select * from pq('batch','web_response_body')))))
union all select 'tls',
  (select count(*) from ((select column_name,column_type from (describe select * from pq('batch','tls')))
                         except (select column_name,column_type from (describe select * from pq('pipeline','tls'))))),
  (select count(*) from ((select column_name,column_type from (describe select * from pq('pipeline','tls')))
                         except (select column_name,column_type from (describe select * from pq('batch','tls')))));

-- ---- 2) RAW full-row diff, excluding only TOP-LEVEL timestamps --------------
--     (nested timestamps / TTL / scan-durations remain -> expected to differ)
select 'dns' dataset,
  (select count(*) from ((select * exclude(crawl_started,crawl_finished,year,month) from pq('batch','dns'))
                         except (select * exclude(crawl_started,crawl_finished,year,month) from pq('pipeline','dns')))) batch_only,
  (select count(*) from ((select * exclude(crawl_started,crawl_finished,year,month) from pq('pipeline','dns'))
                         except (select * exclude(crawl_started,crawl_finished,year,month) from pq('batch','dns')))) pipe_only
union all select 'web',
  (select count(*) from ((select * exclude(crawl_started,crawl_finished,year,month) from pq('batch','web'))
                         except (select * exclude(crawl_started,crawl_finished,year,month) from pq('pipeline','web')))),
  (select count(*) from ((select * exclude(crawl_started,crawl_finished,year,month) from pq('pipeline','web'))
                         except (select * exclude(crawl_started,crawl_finished,year,month) from pq('batch','web'))))
union all select 'web_response_body',
  (select count(*) from ((select * exclude(crawl_started,year,month) from pq('batch','web_response_body'))
                         except (select * exclude(crawl_started,year,month) from pq('pipeline','web_response_body')))),
  (select count(*) from ((select * exclude(crawl_started,year,month) from pq('pipeline','web_response_body'))
                         except (select * exclude(crawl_started,year,month) from pq('batch','web_response_body'))))
union all select 'tls',
  (select count(*) from ((select * exclude(crawl_started,crawl_finished,year,month) from pq('batch','tls'))
                         except (select * exclude(crawl_started,crawl_finished,year,month) from pq('pipeline','tls')))),
  (select count(*) from ((select * exclude(crawl_started,crawl_finished,year,month) from pq('pipeline','tls'))
                         except (select * exclude(crawl_started,crawl_finished,year,month) from pq('batch','tls'))));

-- ---- 3) STABLE semantic content diff (expect 0 differences both ways) --------
select 'dns_request' chk,
  (select count(*) from (from dns_req('batch') except from dns_req('pipeline'))) batch_only,
  (select count(*) from (from dns_req('pipeline') except from dns_req('batch'))) pipe_only
union all select 'dns_response',
  (select count(*) from (from dns_resp('batch') except from dns_resp('pipeline'))),
  (select count(*) from (from dns_resp('pipeline') except from dns_resp('batch')))
union all select 'web_page_visits',
  (select count(*) from (from web_pv('batch') except from web_pv('pipeline'))),
  (select count(*) from (from web_pv('pipeline') except from web_pv('batch')))
union all select 'web_response_body',
  (select count(*) from (from wrb('batch') except from wrb('pipeline'))),
  (select count(*) from (from wrb('pipeline') except from wrb('batch')))
union all select 'tls_visits',
  (select count(*) from (from tls_v('batch') except from tls_v('pipeline'))),
  (select count(*) from (from tls_v('pipeline') except from tls_v('batch')));

