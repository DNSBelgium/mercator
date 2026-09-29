create or replace macro pq(side, ds) as table
  select * from read_parquet(printf('/tmp/mercator-e2e/%s/data/%s/**/*.parquet', side, ds));

create or replace macro web_pv(side) as table
  with a as (select visit_id, domain_name, matching_url, vat_values, visited_urls, unnest(page_visits) as pv from pq(side,'web'))
  select pv.path,
         pv.url,
         pv.final_url,
         pv.status_code,
         pv.content_length,
         len(pv.detected_technologies) as n_tech,
         pv.detected_technologies,
         visited_urls,
         vat_values
  from a;

.mode box
.maxwidth 200
select 'BATCH' src, * from web_pv('batch') order by path;
select 'PIPE'  src, * from web_pv('pipeline') order by path;

