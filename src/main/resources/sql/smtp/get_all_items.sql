WITH
    visits_result1 AS (
        SELECT * EXCLUDE (year, month)
        FROM read_parquet(coalesce(getvariable('smtpVisitDestination'), '~/mercator/data/smtp/visits') || '/**/*/*.parquet', union_by_name=True)
    ),
    visits_result AS (
        SELECT * replace(epoch(timestamp) as timestamp)
        FROM visits_result1
    ),
    hosts_result AS (
        SELECT * EXCLUDE (year, month)
        FROM read_parquet(coalesce(getvariable('smtpHostDestination'), '~/mercator/data/smtp/hosts') || '/**/*/*.parquet', union_by_name=True)
    ),
    conversation_struct as (
        select visit_id,
               id,
               from_mx,
               host_name,
               priority,
               struct_pack(
                       ip := conversation_ip,
                       asn := conversation_asn,
                       country := conversation_country,
                       asn_organisation := conversation_asn_organisation,
                       banner := conversation_banner,
                       connect_ok := conversation_connect_ok,
                       connect_reply_code := conversation_connect_reply_code,
                       supported_extensions := conversation_supported_extensions,
                       ip_version := conversation_ip_version,
                       start_tls_ok := conversation_start_tls_ok,
                       start_tls_reply_code := conversation_start_tls_reply_code,
                       error_message := conversation_error_message,
                       error := conversation_error,
                       connection_time_ms := conversation_connection_time_ms,
                       software := conversation_software,
                       software_version := conversation_software_version,
                       "timestamp" := epoch(conversation_timestamp)
               ) as conversation
        from hosts_result
    ),
    host_struct as (
        select
            visit_id,
            struct_pack(
                    id := id,
                    from_mx := from_mx,
                    host_name := host_name,
                    priority := priority,
                    conversation := conversation) as host
        from conversation_struct
    ),
    smtp_visit as (
        select  host_struct.visit_id,
                visits_result.domain_name,
                timestamp, num_conversations,
                list(host) as hosts,
                crawl_status

        from host_struct INNER JOIN visits_result
        on host_struct.visit_id = visits_result.visit_id
            group by all

    )
select * from smtp_visit