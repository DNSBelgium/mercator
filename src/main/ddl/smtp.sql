create table smtp_crawler.smtp_conversation
(
    id                   serial
        primary key,
    ip                   varchar(64) not null,
    asn                  bigint,
    country              varchar(256),
    asn_organisation     varchar(128),
    banner               varchar(512),
    connect_ok           boolean,
    connect_reply_code   integer,
    ip_version           smallint,
    start_tls_ok         boolean,
    start_tls_reply_code integer,
    error_message        varchar(256),
    error                varchar(64),
    connection_time_ms   bigint,
    software             varchar(128),
    software_version     varchar(128),
    timestamp            timestamp with time zone,
    extensions           jsonb
);

create table smtp_crawler.smtp_visit
(
    visit_id          varchar                     not null
        primary key,
    domain_name       varchar(128)             not null,
    timestamp         timestamp with time zone not null,
    num_conversations integer                  not null,
    crawl_status      varchar(64)
);

create table smtp_crawler.smtp_host
(
    id           serial
        primary key,
    visit_id     varchar         not null
        references smtp_crawler.smtp_visit,
    from_mx      boolean,
    host_name    varchar(128) not null,
    priority     integer      not null,
    conversation integer
        references smtp_crawler.smtp_conversation,
    unique (visit_id, host_name, conversation, priority)
);


