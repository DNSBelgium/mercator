with data as (
    select visit_id
         , domain_name
         , jsonb_array_elements(technologies) ->> 'name'                                         tech_name
         , jsonb_array_elements(jsonb_array_elements(technologies) -> 'categories') ->> 'slug'   cat_slug
    from content_crawler.wappalyzer_result
)
select *
from data
where cat_slug = 'ecommerce'

