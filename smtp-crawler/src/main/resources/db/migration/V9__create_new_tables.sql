CREATE TABLE smtp_conversation
(
    id                   SERIAL PRIMARY KEY,
    ip                   VARCHAR(64) NOT NULL,
    asn                  bigint,
    country              VARCHAR(256),
    asn_organisation     VARCHAR(128),
    banner               VARCHAR(512),
    connect_ok           BOOLEAN,
    connect_reply_code   int,
    ip_version           smallint,
    start_tls_ok         BOOLEAN,
    start_tls_reply_code int,
    error_message        VARCHAR(256),
    error                VARCHAR(64),
    connection_time_ms   bigint,
    software             VARCHAR(128),
    software_version     VARCHAR(128),
    timestamp            timestamp with time zone,
    extensions           jsonb
);

create table smtp_visit
(
    visit_id            UUID                primary key,
    domain_name         varchar(128)        not null,
    timestamp           timestamp           not null,
    num_conversations   integer             not null,
    crawl_status        VARCHAR(64)
);

create table smtp_host
(
    id                  serial          PRIMARY KEY,
    visit_id            UUID            NOT NULL        REFERENCES smtp_visit,
    from_mx             boolean,
    host_name           VARCHAR(128)    NOT NULL,
    priority            int             NOT NULL,
    conversation        int                             REFERENCES smtp_conversation,
    status              VARCHAR(64),
    unique (visit_id, host_name, conversation, priority)
);