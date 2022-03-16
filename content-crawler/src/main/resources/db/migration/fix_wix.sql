-- See https://jira.office.dnsbelgium.be/jira/browse/PA-14406

-- Wix was wrongly considered as an e-commerce technology
-- The following data fixes technology column in  content_crawler.wappalyzer_result for the the impacted rows

create table wix_needs_fix
as
select
    w.visit_id
     , w.technologies
from content_crawler.wappalyzer_result w
where w.technologies @> '[ {"name": "Wix"} ]' ::jsonb
;

create index wix_needs_fix_visit_id  on wix_needs_fix(visit_id);

-- expected: 1,594,991 rows affected in 9 s 837 ms
create table wix_fixed as
with details as (
    -- decompose JSON into separate columns
    select w.visit_id
         , jsonb_array_elements(technologies) ->> 'cpe'                         as cpe
         , jsonb_array_elements(technologies) ->> 'icon'                        as icon
         , jsonb_array_elements(technologies) ->> 'name'                        as name
         , jsonb_array_elements(technologies) ->> 'slug'                        as slug
         , jsonb_array_elements(technologies) ->> 'version'                     as version
         , jsonb_array_elements(technologies) ->> 'categories'                  as categories
         , jsonb_array_elements(technologies) ->> 'website'                     as website
         , cast(jsonb_array_elements(technologies) ->> 'confidence' as numeric) as confidence
    from wix_needs_fix w
)
-- fix the categories when it's Wix
select w.visit_id
     , w.cpe
     , w.icon
     , w.name
     , w.slug
     , w.version
     , w.website
     , case
           when (w.name = 'Wix') then
               jsonb('[{"id": 1, "name": "CMS", "slug": "cms"}, {"id": 11, "name": "Blogs", "slug": "blogs"}]')
           else
               jsonb(categories)
    end as categories
     , w.confidence
from details w;

create index wix_fixed_visit_id on wix_fixed(visit_id);

create table wix_fixed_json
as
select visit_id
     , jsonb_agg(wix_fixed)
           #- '{0,visit_id}'  #- '{1,visit_id}'  #- '{2,visit_id}'  #- '{3,visit_id}'
           #- '{4,visit_id}'  #- '{5,visit_id}'  #- '{6,visit_id}'  #- '{7,visit_id}'
           #- '{8,visit_id}'  #- '{9,visit_id}'  #- '{10,visit_id}' #- '{11,visit_id}'
           #- '{12,visit_id}' #- '{13,visit_id}' #- '{14,visit_id}' #- '{15,visit_id}'
           #- '{16,visit_id}' #- '{17,visit_id}' #- '{18,visit_id}' #- '{19,visit_id}'
           #- '{20,visit_id}' #- '{21,visit_id}' #- '{22,visit_id}' #- '{23,visit_id}'
           #- '{24,visit_id}' #- '{25,visit_id}' #- '{26,visit_id}' #- '{27,visit_id}'
           #- '{28,visit_id}' #- '{29,visit_id}' #- '{30,visit_id}' #- '{31,visit_id}'
    as technologies
from wix_fixed
--where 1=2
group by visit_id;

create index wix_fixed_json_visit_id on wix_fixed_json(visit_id);

-- Expected: 199,744 rows affected in 1 m 40 s 511 ms

update content_crawler.wappalyzer_result w
set technologies = fix.technologies
from wix_fixed_json fix
where w.visit_id = fix.visit_id
;

drop table wix_needs_fix;
drop table wix_fixed;
drop table wix_fixed_json;