-- To speed up some queries you can create a table with all websites that have ecommerce technology
create table ecommerce
as
with data as (
    select visit_id
         , domain_name
         , jsonb_array_elements(technologies) ->> 'name'                                        tech_name
         , jsonb_array_elements(jsonb_array_elements(technologies) -> 'categories') ->> 'slug' cat_slug
    from content_crawler.wappalyzer_result
)
select
    visit_id
     , domain_name
     , tech_name as tech0
     , row_number() over (partition by visit_id order by case when(tech_name = 'Cart Functionality') then 0 else 1 end) as rownr
     , count(1)     over (partition by visit_id) as count
     , lead(tech_name, 1)     over (partition by visit_id order by case when(tech_name = 'Cart Functionality') then 0 else 1 end, tech_name) as tech1
     , lead(tech_name, 2)     over (partition by visit_id order by case when(tech_name = 'Cart Functionality') then 0 else 1 end, tech_name) as tech2
     , lead(tech_name, 3)     over (partition by visit_id order by case when(tech_name = 'Cart Functionality') then 0 else 1 end, tech_name) as tech3
     , lead(tech_name, 4)     over (partition by visit_id order by case when(tech_name = 'Cart Functionality') then 0 else 1 end, tech_name) as tech4
     , lead(tech_name, 5)     over (partition by visit_id order by case when(tech_name = 'Cart Functionality') then 0 else 1 end, tech_name) as tech5
from data
where cat_slug = 'ecommerce'
;