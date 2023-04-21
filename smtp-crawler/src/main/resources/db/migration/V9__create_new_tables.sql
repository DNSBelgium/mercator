CREATE TABLE smtp_conversation
(
    id                   SERIAL PRIMARY KEY,
    ip                   VARCHAR(64) NOT NULL,
    asn                  bigint,
    country              VARCHAR(256),
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
    timestamp            timestamp with time zone,
    extensions           jsonb
);

create table smtp_visit
(
    visit_id            UUID                primary key,
    domain_name         varchar(128)        not null,
    timestamp           timestamp           not null,
    num_conversations   integer             not null
);

create table smtp_host
(
    visit_id            UUID            NOT NULL        REFERENCES smtp_visit,
    from_mx             boolean         NOT NULL,
    host_name           VARCHAR(128)    NOT NULL,
    priority            int             NOT NULL,
    conversation        int             NOT NULL        REFERENCES smtp_conversation,
    primary key(visit_id, host_name, conversation)
);