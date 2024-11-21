create table tls_crawler.certificate
(
    sha256_fingerprint       varchar(256) primary key,
    version                  integer      not null,
    public_key_schema        varchar(256),
    public_key_length        integer,
    not_before               timestamp with time zone,
    not_after                timestamp with time zone,
    issuer                   varchar(500),
    subject                  varchar(500),
    signature_hash_algorithm varchar(256),
    signed_by_sha256         varchar(256)  references tls_crawler.certificate,
    subject_alt_names        jsonb,
    serial_number_hex        varchar(64),
    create_timestamp         timestamp default CURRENT_TIMESTAMP
);

create table tls_full_scan
(
    id                        integer not null            primary key,
    crawl_timestamp           timestamp with time zone                                not null,
    ip                        varchar(255),
    server_name               varchar(128)                                            not null,
    connect_ok                boolean                                                 not null,
    support_tls_1_3           boolean,
    support_tls_1_2           boolean,
    support_tls_1_1           boolean,
    support_tls_1_0           boolean,
    support_ssl_3_0           boolean,
    support_ssl_2_0           boolean,
    selected_cipher_tls_1_3   varchar,
    selected_cipher_tls_1_2   varchar,
    selected_cipher_tls_1_1   varchar,
    selected_cipher_tls_1_0   varchar,
    selected_cipher_ssl_3_0   varchar,
    accepted_ciphers_ssl_2_0  varchar[],
    lowest_version_supported  varchar,
    highest_version_supported varchar,
    error_tls_1_3             varchar,
    error_tls_1_2             varchar,
    error_tls_1_1             varchar,
    error_tls_1_0             varchar,
    error_ssl_3_0             varchar,
    error_ssl_2_0             varchar,
    millis_ssl_2_0            integer,
    millis_ssl_3_0            integer,
    millis_tls_1_0            integer,
    millis_tls_1_1            integer,
    millis_tls_1_2            integer,
    millis_tls_1_3            integer,
    total_duration_in_ms      integer
);

create table tls_crawl_result
(
    id                             bigint                          primary key,
    visit_id                       varchar(26)                     not null,
    domain_name                    varchar(128)                    not null,
    crawl_timestamp                timestamp with time zone        not null,
    full_scan                      integer                         not null  references tls_full_scan,
    host_name_matches_certificate  boolean,
    host_name                      varchar(128)                    not null,
    leaf_certificate               varchar(256),
    certificate_expired            boolean,
    certificate_too_soon           boolean,
    chain_trusted_by_java_platform boolean
);

create table blacklist_entry
(
    cidr_prefix varchar(256)    primary key
);

