"""initial version of the schema

Revision ID: e3b9c157df1f
Revises: 
Create Date: 2021-12-15 09:43:48.828126

"""
from alembic import op

# revision identifiers, used by Alembic.
revision = 'e3b9c157df1f'
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""
create table ssl_crawl_result
(
    id                                       serial primary key       not null,
    visit_id                                 uuid                     not null,
    domain_name                              varchar(256)             not null,
    crawl_timestamp                          timestamp with time zone not null,
    ip_address                               varchar(256),
    ok                                       boolean                  not null,
    problem                                  text,
    hostname_used_for_server_name_indication varchar(256),
    nb_certificate_deployed                  integer,
    support_ssl_2_0                          boolean,
    support_ssl_3_0                          boolean,
    support_tls_1_0                          boolean,
    support_tls_1_1                          boolean,
    support_tls_1_2                          boolean,
    support_tls_1_3                          boolean,
    support_ecdh_key_exchange                boolean -- new, same idea as the support_tls_x_x column
    -- True if the server supports at least one cipher suite with an ECDH key exchange
);

create table certificate
(
    id                       serial primary key not null,
    version                  varchar(8),
    serial_number            varchar(64) unique,
    public_key_schema        varchar(256),
    public_key_length        integer,
    not_before               timestamp with time zone,
    not_after                timestamp with time zone,
    issuer                   varchar(256),
    subject                  varchar(256),
    signature_hash_algorithm varchar(256),
    signed_by                integer references certificate
);

create table trust_store
(
    id      serial primary key not null,
    name    varchar(256)       not null,
    version varchar(256)       not null,
    unique (name, version)  -- avoid having twice the same trust store with the same version
);

create table certificate_deployment
(
    id                                         serial primary key not null,
    ssl_crawl_result_id                        integer            not null references ssl_crawl_result,
    leaf_certificate_id                        integer            not null references certificate,
    length_received_certificate_chain          integer,
    leaf_certificate_subject_matches_hostname  boolean,
    leaf_certificate_has_must_staple_extension boolean,
    leaf_certificate_is_ev                     boolean,
    received_chain_contains_anchor_certificate boolean,
    received_chain_has_valid_order             boolean,
    verified_chain_has_sha1_signature          boolean,
    verified_chain_has_legacy_symantec_anchor  boolean,
    ocsp_response_is_trusted                   boolean
);

create table check_against_trust_store
(
    certificate_deployment_id integer not null references certificate_deployment,
    trust_store_id            integer not null references trust_store,
    valid                     boolean not null
);

create table curve
(
    id          serial primary key not null,
    name        varchar(256)       not null,
    openssl_nid integer            not null unique
);


-- Might be populated with data from https://ciphersuite.info/blog/2019/04/05/how-to-use-our-api/
-- List all IETF cipher suites for TLS
create table cipher_suite
(
    iana_name                varchar(256) primary key not null,
    openssl_name             varchar(256), -- is not always unique
    key_exchange_algorithm   varchar(256),
    authentication_algorithm varchar(256),
    encryption_algorithm     varchar(256),
    hash_algorithm           varchar(256),
    security                 varchar(256)
);

create table curve_support
(
    ssl_crawl_result_id integer not null references ssl_crawl_result,
    curve_id            integer not null references curve,
    supported           boolean not null
);

create table cipher_suite_support
(
    ssl_crawl_result_id integer      not null references ssl_crawl_result,
    cipher_suite_id     varchar(256) not null references cipher_suite,
    protocol            varchar(256) not null,
    supported           boolean      not null
);

alter table check_against_trust_store
    add primary key (certificate_deployment_id, trust_store_id);
alter table curve_support
    add primary key (ssl_crawl_result_id, curve_id);
alter table cipher_suite_support
    add primary key (ssl_crawl_result_id, cipher_suite_id, protocol); -- add protocol in PK
    """)


def downgrade():
    pass
