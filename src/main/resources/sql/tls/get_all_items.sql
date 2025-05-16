with visits as (
    from read_parquet('${base_location}/**/*.parquet')
),
     nested as (
         select visit_id,
                domain_name,
                host_name,
                host_name_matches_certificate,
                chain_trusted_by_java_platform,
                certificate_expired,
                certificate_too_soon,
                certificate_chain_fingerprints,
                crawl_timestamp,
             {
             crawl_timestamp: full_scan_crawl_timestamp,
             ip: ip,
             server_name: server_name,
             connect_ok: connect_ok,
             support_tls_1_3: support_tls_1_3,
             support_tls_1_2: support_tls_1_2,
             support_tls_1_1: support_tls_1_1,
             support_tls_1_0: support_tls_1_0,
             support_ssl_3_0: support_ssl_3_0,
             support_ssl_2_0: support_ssl_2_0,
             selected_cipher_tls_1_3: selected_cipher_tls_1_3,
             selected_cipher_tls_1_2: selected_cipher_tls_1_2,
             selected_cipher_tls_1_1: selected_cipher_tls_1_1,
             selected_cipher_tls_1_0: selected_cipher_tls_1_0,
             selected_cipher_ssl_3_0: selected_cipher_ssl_3_0,
             lowest_version_supported: lowest_version_supported,
             highest_version_supported: highest_version_supported,
             error_tls_1_3: error_tls_1_3,
             error_tls_1_2: error_tls_1_2,
             error_tls_1_1: error_tls_1_1,
             error_tls_1_0: error_tls_1_0,
             error_ssl_3_0: error_ssl_3_0,
             error_ssl_2_0: error_ssl_2_0,
             millis_tls_1_3: millis_tls_1_3,
             millis_tls_1_2: millis_tls_1_2,
             millis_tls_1_1: millis_tls_1_1,
             millis_tls_1_0: millis_tls_1_0,
             millis_ssl_3_0: millis_ssl_3_0,
             millis_ssl_2_0: millis_ssl_2_0
             } as full_scan_entity
         from visits
     ),
     results as (
         select visit_id,
                domain_name,
                min(crawl_timestamp) as crawl_timestamp,
                list(struct_pack(*columns(*))) as visits
         from nested
         group by 1, 2
     )
select * from results