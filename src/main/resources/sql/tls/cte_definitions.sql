with typed as (
from read_json(
    coalesce(getvariable('jsonLocation'), '~/mercator/json/tls/*.json'),
    columns = {
    visit_id: 'VARCHAR',
    domain_name: 'VARCHAR',
    crawl_started: 'TIMESTAMP',
    crawl_finished: 'TIMESTAMP',
    visits: 'struct(
      full_scan_entity struct(
        crawl_started TIMESTAMP,
        crawl_finished TIMESTAMP,
        ip VARCHAR,
        server_name VARCHAR,
        connect_ok BOOLEAN,
        support_tls_1_3 BOOLEAN,
        support_tls_1_2 BOOLEAN,
        support_tls_1_1 BOOLEAN,
        support_tls_1_0 BOOLEAN,
        support_ssl_3_0 BOOLEAN,
        support_ssl_2_0 BOOLEAN,
        selected_cipher_tls_1_3 VARCHAR,
        selected_cipher_tls_1_2 VARCHAR,
        selected_cipher_tls_1_1 VARCHAR,
        selected_cipher_tls_1_0 VARCHAR,
        selected_cipher_ssl_3_0 VARCHAR,
        lowest_version_supported VARCHAR,
        highest_version_supported VARCHAR,
        error_tls_1_3 VARCHAR,
        error_tls_1_2 VARCHAR,
        error_tls_1_1 VARCHAR,
        error_tls_1_0 VARCHAR,
        error_ssl_3_0 VARCHAR,
        error_ssl_2_0 VARCHAR,
        millis_tls_1_3 BIGINT,
        millis_tls_1_2 BIGINT,
        millis_tls_1_1 BIGINT,
        millis_tls_1_0 BIGINT,
        millis_ssl_3_0 BIGINT,
        millis_ssl_2_0 BIGINT
      ),
      host_name VARCHAR,
      host_name_matches_certificate BOOLEAN,
      chain_trusted_by_java_platform BOOLEAN,
      certificate_expired BOOLEAN,
      certificate_too_soon BOOLEAN,
      crawl_started TIMESTAMP,
      crawl_finished TIMESTAMP,
      certificate_chain_fingerprints VARCHAR[],
      certificate_chain struct(
        version BIGINT,
        serial_number_hex VARCHAR,
        public_key_schema VARCHAR,
        public_key_length BIGINT,
        not_before TIMESTAMP,
        not_after TIMESTAMP,
        issuer VARCHAR,
        subject VARCHAR,
        signature_hash_algorithm VARCHAR,
        sha256_fingerprint VARCHAR,
        subject_alternative_names VARCHAR[]
      )[]
    )[]'
    }
    )
),
added_year_month as (
  select *, year(crawl_started) as year, month(crawl_started) as month
  from typed
)