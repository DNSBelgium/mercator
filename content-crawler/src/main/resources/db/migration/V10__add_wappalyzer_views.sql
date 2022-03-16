create view detected_technologies
as
select result.*
     , jsonb_array_elements(technologies) ->> 'cpe'::text   technology_cpe
     , jsonb_array_elements(technologies) ->> 'slug'::text   technology_slug
     , jsonb_array_elements(technologies) ->> 'icon'::text   technology_icon
     , jsonb_array_elements(technologies) ->> 'name'::text   technology_name
     , jsonb_array_elements(technologies) ->> 'version'::text   technology_version
     , jsonb_array_elements(technologies) ->> 'website'::text   technology_website
     , cast(jsonb_array_elements(technologies) ->> 'confidence' as float) as technology_confidence
from content_crawler.wappalyzer_result result;

-- Some slugs have more than 1 category: create separate view to avoid erroneous count queries

create view technologies_with_category
as
select result.*
     , jsonb_array_elements(technologies) ->> 'cpe'::text   technology_cpe
     , jsonb_array_elements(technologies) ->> 'slug'::text   technology_slug
     , jsonb_array_elements(technologies) ->> 'icon'::text   technology_icon
     , jsonb_array_elements(technologies) ->> 'name'::text   technology_name
     , jsonb_array_elements(technologies) ->> 'version'::text   technology_version
     , jsonb_array_elements(technologies) ->> 'website'::text   technology_website
     , cast(jsonb_array_elements(technologies) ->> 'confidence' as float) as technology_confidence
     , jsonb_array_elements(
                   jsonb_array_elements(technologies) -> 'categories') ->> 'name' as category_name
     , jsonb_array_elements(
                   jsonb_array_elements(technologies) -> 'categories') ->> 'id' as category_id
     , jsonb_array_elements(
                   jsonb_array_elements(technologies) -> 'categories') ->> 'slug' as category_slug
from content_crawler.wappalyzer_result result;
