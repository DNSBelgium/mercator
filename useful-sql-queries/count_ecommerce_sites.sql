-- warning this query can take a few minutes when you have a lot of data
-- you might want to consider creating a materialised view based on the data in content_crawler.wappalyzer_result
with data as (
    select visit_id
         , domain_name
         , jsonb_array_elements(technologies) ->> 'name'                                        tech_name
         , jsonb_array_elements(jsonb_array_elements(technologies) -> 'categories') ->> 'slug' cat_slug
    from content_crawler.wappalyzer_result
    join dispatcher.dispatcher_event_labels l on l.visit_id = t.visit_id
    where l.labels in ('todo')
)
select labels, count(distinct domain_name) as count
from data
where cat_slug = 'ecommerce'
group by labels
