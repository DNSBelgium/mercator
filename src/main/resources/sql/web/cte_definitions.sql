with
    json_data as (
        select *
        from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/web/*.json'), field_appearance_threshold=1)
    ),
    all_results as (
        select
            visit_id                      ::varchar            as visit_id,
            domain_name                   ::varchar            as domain_name,
            start_url                     ::varchar            as start_url,
            matching_url                  ::varchar            as matching_url,
            crawl_started                 ::timestamp          as crawl_started,
            crawl_finished                ::timestamp          as crawl_finished,
            vat_values                    ::varchar[]          as vat_values,
            visited_urls                  ::varchar[]          as visited_urls,
            page_visits                                        as page_visits,
            html_features                                      as html_features,
            detected_technologies         ::varchar[]          as detected_technologies,
            year(crawl_started::timestamp)  as year,
            month(crawl_started::timestamp) as month
        from json_data
    ),
    web_crawl_result as (
        select
            visit_id,
            domain_name,
            start_url,
            matching_url,
            crawl_started,
            crawl_finished,
            vat_values,
            visited_urls,
            detected_technologies,
            year,
            month
        from all_results
    ),
    page_visits_unnested as (
        select
            visit_id,
            domain_name,
            unnest(page_visits, recursive:=True),
            year,
            month
        from all_results
    ),
    page_visits as (
        select
            visit_id                      ::VARCHAR            as visit_id,
            domain_name                   ::VARCHAR            as domain_name,
            crawl_started                 ::TIMESTAMP          as crawl_started,
            crawl_finished                ::TIMESTAMP          as crawl_finished,
            status_code                   ::INTEGER            as status_code,
            url                           ::VARCHAR            as url,
            link_text                     ::VARCHAR            as link_text,
            path                          ::VARCHAR            as path,
            response_body                 ::VARCHAR            as response_body,
            vat_values                    ::VARCHAR[]          as vat_values,
            year,
            month
        from page_visits_unnested
    ),
    html_features_unnested as (
        select
            visit_id,
            domain_name,
            unnest(html_features, recursive:=True),
            year,
            month
        from all_results
    ),
    html_features_casted as (
        select
            visit_id                              ::VARCHAR     as visit_id,
            crawl_timestamp                       ::TIMESTAMP   as crawl_timestamp,
            domain_name                           ::VARCHAR     as domain_name,
            url                                   ::VARCHAR     as url,
            nb_imgs                               ::INTEGER     as nb_imgs,
            nb_links_int                          ::INTEGER     as nb_links_int,
            nb_links_ext                          ::INTEGER     as nb_links_ext,
            nb_links_tel                          ::INTEGER     as nb_links_tel,
            nb_links_email                        ::INTEGER     as nb_links_email,
            nb_input_txt                          ::INTEGER     as nb_input_txt,
            nb_button                             ::INTEGER     as nb_button,
            nb_meta_desc                          ::INTEGER     as nb_meta_desc,
            nb_meta_keyw                          ::INTEGER     as nb_meta_keyw,
            nb_numerical_strings                  ::INTEGER     as nb_numerical_strings,
            nb_tags                               ::INTEGER     as nb_tags,
            nb_words                              ::INTEGER     as nb_words,
            nb_letters                            ::INTEGER     as nb_letters,
            html_length                           ::INTEGER     as html_length,
            nb_facebook_shallow_links             ::INTEGER     as nb_facebook_shallow_links,
            nb_facebook_deep_links                ::INTEGER     as nb_facebook_deep_links,
            nb_linkedin_deep_links                ::INTEGER     as nb_linkedin_deep_links,
            nb_linkedin_shallow_links             ::INTEGER     as nb_linkedin_shallow_links,
            nb_twitter_deep_links                 ::INTEGER     as nb_twitter_deep_links,
            nb_twitter_shallow_links              ::INTEGER     as nb_twitter_shallow_links,
            nb_youtube_deep_links                 ::INTEGER     as nb_youtube_deep_links,
            nb_youtube_shallow_links              ::INTEGER     as nb_youtube_shallow_links,
            nb_vimeo_deep_links                   ::INTEGER     as nb_vimeo_deep_links,
            nb_vimeo_shallow_links                ::INTEGER     as nb_vimeo_shallow_links,
            nb_currency_names                     ::INTEGER     as nb_currency_names,
            nb_distinct_currencies                ::INTEGER     as nb_distinct_currencies,
            distance_title_final_dn               ::INTEGER     as distance_title_final_dn,
            distance_title_initial_dn             ::INTEGER     as distance_title_initial_dn,
            longest_subsequence_title_final_dn    ::INTEGER     as longest_subsequence_title_final_dn,
            longest_subsequence_title_initial_dn  ::INTEGER     as longest_subsequence_title_initial_dn,
            fraction_words_title_final_dn         ::DOUBLE      as fraction_words_title_final_dn,
            fraction_words_title_initial_dn       ::DOUBLE      as fraction_words_title_initial_dn,
            nb_distinct_words_in_title            ::INTEGER     as nb_distinct_words_in_title,
            body_text_language                    ::VARCHAR     as body_text_language,
            body_text_language_2                  ::VARCHAR     as body_text_language_2,
            nb_distinct_hosts_in_urls             ::INTEGER     as nb_distinct_hosts_in_urls,
            external_hosts                        ::VARCHAR[]   as external_hosts,
            facebook_links                        ::VARCHAR[]   as facebook_links,
            twitter_links                         ::VARCHAR[]   as twitter_links,
            linkedin_links                        ::VARCHAR[]   as linkedin_links,
            youtube_links                         ::VARCHAR[]   as youtube_links,
            vimeo_links                           ::VARCHAR[]   as vimeo_links,
            title                                 ::VARCHAR     as title,
            htmlstruct                            ::VARCHAR     as htmlstruct,
            body_text                             ::VARCHAR     as body_text,
            meta_text                             ::VARCHAR     as meta_text,
            body_text_truncated                   ::BOOLEAN     as body_text_truncated,
            meta_text_truncated                   ::BOOLEAN     as meta_text_truncated,
            title_truncated                       ::BOOLEAN     as title_truncated,
            year,
            month
        from html_features_unnested
    )