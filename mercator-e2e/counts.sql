create or replace macro pq(side, ds) as table
  select * from read_parquet(printf('/tmp/mercator-e2e/%s/data/%s/**/*.parquet', side, ds));

.mode box
select 'dns' ds,
       (select count(*) from pq('batch','dns'))    batch_rows,
       (select count(*) from pq('pipeline','dns'))  pipe_rows
union all
select 'web',
       (select count(*) from pq('batch','web')),
       (select count(*) from pq('pipeline','web'))
union all
select 'web_response_body',
       (select count(*) from pq('batch','web_response_body')),
       (select count(*) from pq('pipeline','web_response_body'))
union all
select 'tls',
       (select count(*) from pq('batch','tls')),
       (select count(*) from pq('pipeline','tls'));

