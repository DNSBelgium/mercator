-- we will drop this table in a later migration script
create table duplicate_html_features_id
as
select * from (
                  select h.id
                       , count(1) over (partition by visit_id, url) count
                       , rank() over (partition by visit_id, url order by id) rank
                  from html_features h
              ) dups
where count > 1
;

delete
from html_features
where id in (select id from duplicate_html_features_id where rank > 1);

alter table html_features
    add constraint html_visit_url_uq unique (visit_id, url);
