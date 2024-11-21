CREATE SEQUENCE serial START 1;

create table dns_request
(
    id               INTEGER                  primary key DEFAULT nextval('serial'),
    visit_id         varchar(26)              not null,
    domain_name      varchar(128)             not null,
    prefix           varchar(63)              not null,
    record_type      char(10)                 not null,
    rcode            integer,
    crawl_timestamp  timestamp with time zone not null,
    ok               boolean,
    problem          text,
    num_of_responses integer                  not null
);

create table dns_response
(
    dns_request         integer  not null,
    record_data         text     not null,
    ttl                 integer
);

create table response_geo_ips
(
    dns_response     integer
    asn              varchar(255),
    country          varchar(255),
    ip               varchar(255),
    asn_organisation varchar(128),
    ip_version       integer not null
);

