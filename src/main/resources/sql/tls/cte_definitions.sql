with tls as (
    from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/tls/*.json'), field_appearance_threshold=1)
),
     visits as (
         select unnest(visits, max_depth := 2)
         from tls
     ),
     visits_typed as (
         select visit_id                        ::varchar       as visit_id,
                domain_name                     ::varchar       as domain_name,
                host_name                       ::varchar       as host_name,
                host_name_matches_certificate   ::bool          as host_name_matches_certificate,
                chain_trusted_by_java_platform  ::bool          as chain_trusted_by_java_platform,
                certificate_expired             ::bool          as certificate_expired,
                certificate_too_soon            ::bool          as certificate_too_soon,
                certificate_chain_fingerprints  ::varchar[]     as certificate_chain_fingerprints,
                crawl_timestamp                 ::timestamp     as crawl_timestamp
         from visits
     ),
     full_scan_entity as (
         select unnest(full_scan_entity)
         from visits
     ),
     full_scan_typed as (
         select crawl_timestamp             ::timestamp     as full_scan_crawl_timestamp,
                ip                          ::varchar       as ip,
                server_name                 ::varchar       as server_name,
                connect_ok                  ::bool          as connect_ok,
                support_tls_1_3             ::bool          as support_tls_1_3,
                support_tls_1_2             ::bool          as support_tls_1_2,
                support_tls_1_1             ::bool          as support_tls_1_1,
                support_tls_1_0             ::bool          as support_tls_1_0,
                support_ssl_3_0             ::bool          as support_ssl_3_0,
                support_ssl_2_0             ::bool          as support_ssl_2_0,
                selected_cipher_tls_1_3     ::varchar       as selected_cipher_tls_1_3,
                selected_cipher_tls_1_2     ::varchar       as selected_cipher_tls_1_2,
                selected_cipher_tls_1_1     ::varchar       as selected_cipher_tls_1_1,
                selected_cipher_tls_1_0     ::varchar       as selected_cipher_tls_1_0,
                selected_cipher_ssl_3_0     ::varchar       as selected_cipher_ssl_3_0,
                lowest_version_supported    ::varchar       as lowest_version_supported,
                highest_version_supported   ::varchar       as highest_version_supported,
                error_tls_1_3               ::varchar       as error_tls_1_3,
                error_tls_1_2               ::varchar       as error_tls_1_2,
                error_tls_1_1               ::varchar       as error_tls_1_1,
                error_tls_1_0               ::varchar       as error_tls_1_0,
                error_ssl_3_0               ::varchar       as error_ssl_3_0,
                error_ssl_2_0               ::varchar       as error_ssl_2_0,
                millis_tls_1_3              ::int           as millis_tls_1_3,
                millis_tls_1_2              ::int           as millis_tls_1_2,
                millis_tls_1_1              ::int           as millis_tls_1_1,
                millis_tls_1_0              ::int           as millis_tls_1_0,
                millis_ssl_3_0              ::int           as millis_ssl_3_0,
                millis_ssl_2_0              ::int           as millis_ssl_2_0
         from full_scan_entity
     ),
     certificates_unnested as (
         select unnest(cast(certificate_chain as struct(
                      version int,
                      serial_number_hex varchar,
                      public_key_schema varchar,
                      public_key_length int,
                      not_before timestamp,
                      not_after timestamp,
                      issuer varchar,
                      subject varchar,
                      signature_hash_algorithm varchar,
                      sha256_fingerprint varchar,
                      subject_alternative_names varchar[]
          )[]), max_depth :=2 ) from visits
     ),
     export_visits as (
         select *,
             year(crawl_timestamp)  AS year,
             month(crawl_timestamp) AS month
         from visits_typed
            positional join
            full_scan_typed
     ),
     export_certificates as (
         select distinct(*) from certificates_unnested
     )