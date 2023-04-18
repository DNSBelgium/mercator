CREATE TABLE smtp_host
(
    id                   SERIAL PRIMARY KEY,
    ip                   VARCHAR(64) NOT NULL,
    asn                  bigint,
    country              VARCHAR(64),
    asn_organisation     VARCHAR(128),
    banner               VARCHAR(256),
    connect_ok           BOOLEAN,
    connect_reply_code   int,
    ip_version           smallint,
    start_tls_ok         BOOLEAN,
    start_tls_reply_code int,
    error_message        VARCHAR(256),
    connection_time_ms   bigint,
    software             VARCHAR(128),
    software_version     VARCHAR(128),
    timestamp            timestamp with time zone
);