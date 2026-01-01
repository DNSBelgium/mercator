with typed as (
    select *
    from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/web/*.json'),
                   columns={
                       visit_id: 'VARCHAR',
                       domain_name: 'VARCHAR',
                       crawl_started: 'TIMESTAMP',
                       page_visits: 'STRUCT(
                                    url VARCHAR,
                                    final_url VARCHAR,
                                    response_body VARCHAR
                                )[]'
                       })
),
unnested as (
    select visit_id, domain_name, crawl_started, unnest(page_visits) as page_visit
    from typed
),
response_body as (
    select visit_id, domain_name, crawl_started, page_visit.*
    from unnested
),
response_body_with_year_month as (
    select
        *,
        string_to_array(domain_name, '.')[-1] as tld,
        year(crawl_started) as year,
        month(crawl_started) as month
    from response_body
)
