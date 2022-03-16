from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, create_engine
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, scoped_session

from utils import config


def get_int_from_env(name: str, default: int = 0) -> int:
    try:
        value = int(config.get_env(name, default))
        print(f"{name} => {value}")
        return value
    except ValueError:
        print(f"ValueError for {name} => {default}")
        return default

max_overflow = get_int_from_env("SSL_CRAWLER_CONN_POOL_MAX_OVERFLOW", 0)
pool_timeout = get_int_from_env("SSL_CRAWLER_CONN_POOL_TIMEOUT", 60)
pool_size = get_int_from_env("SSL_CRAWLER_CONN_POOL_SIZE", 2)

engine = create_engine(config.get_db_string(), echo=False,
                       pool_size=pool_size,
                       max_overflow=max_overflow,
                       pool_timeout = pool_timeout,
                       connect_args={'options': f'-csearch_path={config.get_env("SSL_CRAWLER_DB_SCHEMA")}'},
                       pool_pre_ping=True)

Base = declarative_base(engine)
session_factory = sessionmaker(bind=engine)
Session = scoped_session(session_factory)


class SslCrawlResult(Base):
    __tablename__ = 'ssl_crawl_result'

    id = Column("id", Integer, primary_key=True, nullable=False)
    visit_id = Column("visit_id", UUID(as_uuid=True), nullable=False)
    hostname = Column("hostname", String(256), nullable=False)
    domain_name = Column("domain_name", String(256), nullable=False)
    crawl_timestamp = Column("crawl_timestamp", DateTime(timezone=True), nullable=False)
    ip_address = Column("ip_address", String(256), nullable=True)
    ok = Column("ok", Boolean, nullable=False)
    problem = Column("problem", String, nullable=True)
    hostname_used_for_server_name_indication = Column("hostname_used_for_server_name_indication", String(256),
                                                      nullable=True)
    nb_certificate_deployed = Column("nb_certificate_deployed", Integer, nullable=True)
    support_ssl_2_0 = Column("support_ssl_2_0", Boolean, nullable=False, default=False)
    support_ssl_3_0 = Column("support_ssl_3_0", Boolean, nullable=False, default=False)
    support_tls_1_0 = Column("support_tls_1_0", Boolean, nullable=False, default=False)
    support_tls_1_1 = Column("support_tls_1_1", Boolean, nullable=False, default=False)
    support_tls_1_2 = Column("support_tls_1_2", Boolean, nullable=False, default=False)
    support_tls_1_3 = Column("support_tls_1_3", Boolean, nullable=False, default=False)
    support_ecdh_key_exchange = Column("support_ecdh_key_exchange", Boolean, nullable=False, default=False)
    # For now we only set the details when something went wrong (because it's rather large)
    # details = Column("details", JSONB, nullable=True)


class Certificate(Base):
    __tablename__ = 'certificate'

    sha256_fingerprint = Column("sha256_fingerprint", String(256), primary_key=True)
    version = Column("version", String(8), nullable=True)
    serial_number = Column("serial_number", String(64), nullable=True)
    public_key_schema = Column("public_key_schema", String(256), nullable=True)
    public_key_length = Column("public_key_length", Integer, nullable=True)
    not_before = Column("not_before", DateTime(timezone=True), nullable=True)
    not_after = Column("not_after", DateTime(timezone=True), nullable=True)
    issuer = Column("issuer", String(256), nullable=True)
    subject = Column("subject", String(256), nullable=True)
    signature_hash_algorithm = Column("signature_hash_algorithm", String(256), nullable=True)
    signed_by_sha256 = Column("signed_by_sha256", String(256), ForeignKey("certificate.sha256_fingerprint"),
                              nullable=True)
    subject_alt_names = Column("subject_alt_names", JSONB(), nullable=True)


class TrustStore(Base):
    __tablename__ = 'trust_store'

    id = Column("id", Integer, primary_key=True, nullable=False)
    name = Column("name", String(256), nullable=False)
    version = Column("version", String(256), nullable=False)


class CertificateDeployment(Base):
    __tablename__ = 'certificate_deployment'

    id = Column("id", Integer, primary_key=True, nullable=False)
    ssl_crawl_result_id = Column("ssl_crawl_result_id", Integer, ForeignKey("ssl_crawl_result.id"), nullable=False)
    leaf_certificate_sha256 = Column("leaf_certificate_sha256", String(256),
                                     ForeignKey("certificate.sha256_fingerprint"), nullable=True)
    length_received_certificate_chain = Column("length_received_certificate_chain", Integer, nullable=True)
    leaf_certificate_subject_matches_hostname = Column("leaf_certificate_subject_matches_hostname", Boolean,
                                                       nullable=True)
    leaf_certificate_has_must_staple_extension = Column("leaf_certificate_has_must_staple_extension", Boolean,
                                                        nullable=True)
    leaf_certificate_is_ev = Column("leaf_certificate_is_ev", Boolean, nullable=True)
    received_chain_contains_anchor_certificate = Column("received_chain_contains_anchor_certificate", Boolean,
                                                        nullable=True)
    received_chain_has_valid_order = Column("received_chain_has_valid_order", Boolean, nullable=True)
    verified_chain_has_sha1_signature = Column("verified_chain_has_sha1_signature", Boolean, nullable=True)
    verified_chain_has_legacy_symantec_anchor = Column("verified_chain_has_legacy_symantec_anchor", Boolean,
                                                       nullable=True)
    ocsp_response_is_trusted = Column("ocsp_response_is_trusted", Boolean, nullable=True)


class CheckAgainstTrustStore(Base):
    __tablename__ = 'check_against_trust_store'

    certificate_deployment_id = Column("certificate_deployment_id", Integer, ForeignKey("certificate_deployment.id"),
                                       primary_key=True, nullable=False)
    trust_store_id = Column("trust_store_id", Integer, ForeignKey("trust_store.id"), primary_key=True, nullable=False)
    valid = Column("valid", Boolean, nullable=False)


class Curve(Base):
    __tablename__ = 'curve'

    id = Column("id", Integer, primary_key=True, nullable=False)
    name = Column("name", String(256), nullable=False)
    openssl_nid = Column("openssl_nid", Integer, unique=True, nullable=False)


class CipherSuite(Base):
    __tablename__ = 'cipher_suite'

    iana_name = Column("iana_name", String(256), primary_key=True, nullable=False)
    openssl_name = Column("openssl_name", String(256), unique=True, nullable=True)
    key_exchange_algorithm = Column("key_exchange_algorithm", String(256), nullable=True)
    authentication_algorithm = Column("authentication_algorithm", String(256), nullable=True)
    encryption_algorithm = Column("encryption_algorithm", String(256), nullable=True)
    hash_algorithm = Column("hash_algorithm", String(256), nullable=True)
    security = Column("security", String(256), nullable=True)


class CurveSupport(Base):
    __tablename__ = 'curve_support'

    ssl_crawl_result_id = Column("ssl_crawl_result_id", Integer, ForeignKey("ssl_crawl_result.id"), primary_key=True,
                                 nullable=False)
    curve_id = Column("curve_id", Integer, ForeignKey("curve.id"), primary_key=True, nullable=False)
    supported = Column("supported", Boolean, nullable=False)


class CipherSuiteSupport(Base):
    __tablename__ = 'cipher_suite_support'

    ssl_crawl_result_id = Column("ssl_crawl_result_id", Integer, ForeignKey("ssl_crawl_result.id"), primary_key=True,
                                 nullable=False)
    cipher_suite_id = Column("cipher_suite_id", String(256), ForeignKey("cipher_suite.iana_name"), primary_key=True,
                             nullable=False)
    protocol = Column("protocol", String(256), nullable=False)
    supported = Column("supported", Boolean, nullable=False)


def get_session():
    return Session()


def close_session():
    Session.remove()
