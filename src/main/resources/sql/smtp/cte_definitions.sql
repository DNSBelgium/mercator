with
    json_data as (
        select *
        from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/smtp/*.json'), field_appearance_threshold=1)
    ),
    all_results as (
        select *,
               year(to_timestamp(timestamp)) as year,
               month(to_timestamp(timestamp)) as month
        from json_data
    ),
    smtp_visit_results as (
        select
            visit_id                ::VARCHAR       as visit_id,
            domain_name             ::VARCHAR       as domain_name,
            to_timestamp(timestamp)                 as timestamp,
            num_conversations       ::INTEGER       as num_conversations,
            crawl_status            ::VARCHAR       as crawl_status,
            year,
            month
        from all_results
    ),
    extract_unnested_hosts_from_smtp AS (
        SELECT visit_id, UNNEST(hosts, max_depth := 3), year, month
        FROM all_results
    ),
    extract_hosts_and_conversation_from_smtp AS (
        SELECT
            visit_id                ::VARCHAR       as visit_id,
            from_mx                 ::BOOLEAN       as from_mx,
            host_name               ::VARCHAR       as host_name,
            priority                ::INTEGER       as priority,
            ip                      ::VARCHAR       AS conversation_ip,
            asn                     ::BIGINT        AS conversation_asn,
            country                 ::VARCHAR       AS conversation_country,
            asn_organisation        ::VARCHAR       AS conversation_asn_organisation,
            banner                  ::VARCHAR       AS conversation_banner,
            connect_ok              ::BOOLEAN       AS conversation_connect_ok,
            connect_reply_code      ::INTEGER       AS conversation_connect_reply_code,
            supported_extensions    ::VARCHAR[]     AS conversation_supported_extensions,
            ip_version              ::INTEGER       AS conversation_ip_version,
            start_tls_ok            ::BOOLEAN       AS conversation_start_tls_ok,
            start_tls_reply_code    ::INTEGER       AS conversation_start_tls_reply_code,
            error_message           ::VARCHAR       AS conversation_error_message,
            error                   ::VARCHAR       AS conversation_error,
            connection_time_ms      ::BIGINT        AS conversation_connection_time_ms,
            software                ::VARCHAR       AS conversation_software,
            software_version        ::VARCHAR       AS conversation_software_version,
            to_timestamp(timestamp)                 AS conversation_timestamp,
            month,
            year
        FROM extract_unnested_hosts_from_smtp
    ),
    smtp_hosts as (
        SELECT * FROM extract_hosts_and_conversation_from_smtp
    )