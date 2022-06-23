create table certificate
(
    sha256_fingerprint       VARCHAR(256)            primary key,
    version                  integer                 NOT NULL,
    serial_number            varchar(64),
    public_key_schema        varchar(256),
    public_key_length        integer,
    not_before               timestamp with time zone,
    not_after                timestamp with time zone,
    issuer                   varchar(500),
    subject                  varchar(500),
    signature_hash_algorithm varchar(256),
    signed_by_sha256         varchar(256)            references certificate,
    subject_alt_names        jsonb
);


create table scan_result (
      id                          SERIAL PRIMARY KEY
    , crawl_timestamp             timestamp with time zone    NOT NULL
    , ip                          varchar(255)
    , server_name                 varchar(128)                NOT NULL -- the server name used as SNI (Server Name Indication)
    , connect_ok                  BOOLEAN                     NOT NULL
    , support_tls_1_3             BOOLEAN
    , support_tls_1_2             BOOLEAN
    , support_tls_1_1             BOOLEAN
    , support_tls_1_0             BOOLEAN
    , support_ssl_3_0             BOOLEAN
    , support_ssl_2_0             BOOLEAN
    , selected_cipher_tls_1_3     varchar
    , selected_cipher_tls_1_2     varchar
    , selected_cipher_tls_1_1     varchar
    , selected_cipher_tls_1_0     varchar
    , selected_cipher_ssl_3_0     varchar
    , accepted_ciphers_ssl_2_0    JSONB
    , lowest_version_supported    varchar
    , highest_version_supported   varchar
    , error_tls_1_3               varchar
    , error_tls_1_2               varchar
    , error_tls_1_1               varchar
    , error_tls_1_0               varchar
    , error_ssl_3_0               varchar
    , error_ssl_2_0               varchar
    , leaf_certificate            VARCHAR(256)   references certificate
    , certificate_expired         BOOLEAN
    , certificate_too_soon        BOOLEAN
);

create table tls_scan_result
(
    id                SERIAL                      PRIMARY KEY
  , visit_id          UUID                        NOT NULL
  , prefix            VARCHAR(20)                 NULL
  , domain_name       VARCHAR(128)                NOT NULL
  , crawl_timestamp   timestamp with time zone    NOT NULL
  , scan_result       int                         NULL     references scan_result
  , hostname_matches_subject                      BOOLEAN
  , CONSTRAINT visit_id_prefix_unique UNIQUE (visit_id, prefix)
);

create unique index tls_scan_result_visitid_prefix_index on tls_scan_result(visit_id, prefix);

