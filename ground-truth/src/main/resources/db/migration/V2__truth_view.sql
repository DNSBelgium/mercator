-- create a view for a join between 3 tables that we often need

create view labeled
as
select lv.visit_id  as visit_id
     , lv.username  as label_username
     , lv.timestamp as label_timestamp
     , l.id as labeld_id
     , l.label as label
     , ctx.id as ctx_id
     , ctx.name as ctx_name
     , ctx.version as ctx_version
     , ctx.description as ctx_description
from ground_truth.labeled_visits lv
         join ground_truth.labels l on lv.label = l.id
         join ground_truth.labeling_context ctx on l.labeling_context_id = ctx.id
;