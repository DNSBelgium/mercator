create table duplicate_content_crawl_id
as
select * from (
                  select c.id
                       , count(1) over (partition by visit_id, url) count
                       , rank() over (partition by visit_id, url order by ok desc, id) rank
                  from content_crawl_result c
              ) dups
where count > 1
;

delete from content_crawl_result
where id in (select id from duplicate_content_crawl_id where rank > 1);

alter table content_crawl_result
  add constraint content_crawler_visitid_url_uq unique (visit_id, url);

