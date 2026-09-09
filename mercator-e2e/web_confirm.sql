create or replace macro pq(side, ds) as table
  select * from read_parquet(printf('/tmp/mercator-e2e/%s/data/%s/**/*.parquet', side, ds));

-- same stable web projection, but with the technologies list SORTED (order-insensitive)
create or replace macro web_pv_sorted(side) as table
  with a as (select visit_id, domain_name, matching_url, vat_values, visited_urls, unnest(page_visits) as pv from pq(side,'web'))
  select visit_id, domain_name, matching_url, vat_values, visited_urls,
         pv.url, pv.final_url, pv.status_code, pv.path, pv.content_length,
         list_sort(pv.detected_technologies) as detected_technologies, pv.vat_values as pv_vat
  from a;

.mode box
select 'web_page_visits (tech sorted)' chk,
  (select count(*) from (from web_pv_sorted('batch') except from web_pv_sorted('pipeline'))) batch_only,
  (select count(*) from (from web_pv_sorted('pipeline') except from web_pv_sorted('batch'))) pipe_only;

