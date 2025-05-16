with
    crawl_result  as (
        select * exclude (year, month)
        from read_parquet(coalesce(getvariable('webCrawlDestination'), '~/mercator/data/web/crawl_result') || '/**/*.parquet', union_by_name=True)
    ),
    html_feature  as (
        select * exclude (year, month)
        from read_parquet(coalesce(getvariable('featuresDestination'), '~/mercator/data/web/html_features') || '/**/*.parquet', union_by_name=True)
    ),
    page_visit    as (
        select * exclude (year, month)
        from read_parquet(coalesce(getvariable('pageVisitDestination'), '~/mercator/data/web/page_visit') || '/**/*.parquet', union_by_name=True)
    ),
    features_per_visit as (
        select visit_id,
               list(html_feature order by crawl_timestamp) as html_features
        from html_feature
        group by visit_id
    ),
    pages_per_visit as (
        select visit_id,
               list(page_visit order by crawl_started) as page_visits
        from page_visit
        group by visit_id
    ),
    combined as (
        select
            crawl_result.*,
            coalesce(features_per_visit.html_features, []) as html_features,
            coalesce(pages_per_visit.page_visits, []) as page_visits
        from crawl_result
                 left join features_per_visit on crawl_result.visit_id = features_per_visit.visit_id
                 left join pages_per_visit    on crawl_result.visit_id = pages_per_visit.visit_id
    )
    from combined