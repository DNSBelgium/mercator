create table content_crawl_result
(
    id                serial
        primary key,
    visit_id          varchar         not null,
    domain_name       varchar(128) not null,
    url               varchar(255) not null,
    ok                boolean      not null,
    problem           text,
    metrics_json      text,
    final_url         varchar(2100),
    html_length       integer,
    crawl_timestamp   timestamp with time zone,
    ipv4              varchar(255),
    ipv6              varchar(255),
    browser_version   varchar(255),
    bucket            varchar(255),
    html_key          varchar(600),
    screenshot_key    varchar(600),
    har_key           varchar(600),
    retries           integer      not null,
    html_status       text,
    screenshot_status text,
    constraint content_crawler_visitid_url_uq
        unique (visit_id, url)
);

create table wappalyzer_result
(
    visit_id     varchar         not null
        primary key,
    domain_name  varchar(128) not null,
    url          varchar(255) not null,
    ok           boolean      not null,
    technologies jsonb,
    error        text,
    urls         jsonb
);

create view metrics
            (id, visit_id, domain_name, url, ok, problem, metrics_json, final_url, html_length, crawl_timestamp, ipv4,
             ipv6, browser_version, bucket, html_key, screenshot_key, har_key, retries, html_status, screenshot_status,
             nodes, documents, frames, jseventlisteners, timestamp, layoutcount, recalcstylecount, recalcstyleduration,
             scriptduration, layoutduration, taskduration, jsheapusedsize, jsheaptotalsize)
as
SELECT r.id,
       r.visit_id,
       r.domain_name,
       r.url,
       r.ok,
       r.problem,
       r.metrics_json,
       r.final_url,
       r.html_length,
       r.crawl_timestamp,
       r.ipv4,
       r.ipv6,
       r.browser_version,
       r.bucket,
       r.html_key,
       r.screenshot_key,
       r.har_key,
       r.retries,
       r.html_status,
       r.screenshot_status,
       (r.metrics_json::json ->> 'Nodes'::text)::integer AS nodes,
        (r.metrics_json::json ->> 'Documents'::text)::integer AS documents,
        (r.metrics_json::json ->> 'Frames'::text)::integer AS frames,
        (r.metrics_json::json ->> 'JSEventListeners'::text)::integer AS jseventlisteners,
        (r.metrics_json::json ->> 'Timestamp'::text)::double precision AS "timestamp",
    (r.metrics_json::json ->> 'LayoutCount'::text)::integer AS layoutcount,
    (r.metrics_json::json ->> 'RecalcStyleCount'::text)::integer AS recalcstylecount,
    (r.metrics_json::json ->> 'RecalcStyleDuration'::text)::double precision AS recalcstyleduration,
    (r.metrics_json::json ->> 'ScriptDuration'::text)::double precision AS scriptduration,
    (r.metrics_json::json ->> 'LayoutDuration'::text)::double precision AS layoutduration,
    (r.metrics_json::json ->> 'TaskDuration'::text)::double precision AS taskduration,
    (r.metrics_json::json ->> 'JSHeapUsedSize'::text)::integer AS jsheapusedsize,
    (r.metrics_json::json ->> 'JSHeapTotalSize'::text)::integer AS jsheaptotalsize
   FROM content_crawler.content_crawl_result r;

create view detected_technologies
            (visit_id, domain_name, url, ok, technologies, error, urls, technology_cpe, technology_slug,
             technology_icon, technology_name, technology_version, technology_website, technology_confidence)
as
SELECT result.visit_id,
       result.domain_name,
       result.url,
       result.ok,
       result.technologies,
       result.error,
       result.urls,
       jsonb_array_elements(result.technologies) ->> 'cpe'::text AS technology_cpe,
        jsonb_array_elements(result.technologies) ->> 'slug'::text AS technology_slug,
        jsonb_array_elements(result.technologies) ->> 'icon'::text AS technology_icon,
        jsonb_array_elements(result.technologies) ->> 'name'::text AS technology_name,
        jsonb_array_elements(result.technologies) ->> 'version'::text AS technology_version,
        jsonb_array_elements(result.technologies) ->> 'website'::text AS technology_website,
        (jsonb_array_elements(result.technologies) ->> 'confidence'::text)::double precision AS technology_confidence
        FROM content_crawler.wappalyzer_result result;

create view technologies_with_category
            (visit_id, domain_name, url, ok, technologies, error, urls, technology_cpe, technology_slug,
             technology_icon, technology_name, technology_version, technology_website, technology_confidence,
             category_name, category_id, category_slug)
as
SELECT result.visit_id,
       result.domain_name,
       result.url,
       result.ok,
       result.technologies,
       result.error,
       result.urls,
       jsonb_array_elements(result.technologies) ->> 'cpe'::text AS technology_cpe,
        jsonb_array_elements(result.technologies) ->> 'slug'::text AS technology_slug,
        jsonb_array_elements(result.technologies) ->> 'icon'::text AS technology_icon,
        jsonb_array_elements(result.technologies) ->> 'name'::text AS technology_name,
        jsonb_array_elements(result.technologies) ->> 'version'::text AS technology_version,
        jsonb_array_elements(result.technologies) ->> 'website'::text AS technology_website,
        (jsonb_array_elements(result.technologies) ->> 'confidence'::text)::double precision AS technology_confidence,
        jsonb_array_elements(jsonb_array_elements(result.technologies) -> 'categories'::text) ->> 'name'::text AS category_name,
        jsonb_array_elements(jsonb_array_elements(result.technologies) -> 'categories'::text) ->> 'id'::text AS category_id,
        jsonb_array_elements(jsonb_array_elements(result.technologies) -> 'categories'::text) ->> 'slug'::text AS category_slug
        FROM content_crawler.wappalyzer_result result;

