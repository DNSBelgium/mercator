from __future__ import annotations

import datetime
import hashlib
import time
import traceback
from typing import List

from cryptography.hazmat.primitives.serialization import Encoding
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from sslyze import (
    ServerScanStatusEnum, ScanCommandAttemptStatusEnum, TlsVersionEnum, ServerScanResult,
    SupportedEllipticCurvesScanResult, EllipticCurve, CipherSuitesScanResult, CipherSuiteAcceptedByServer,
    CipherSuiteRejectedByServer, CertificateInfoScanResult, CertificateDeploymentAnalysisResult
)
from sslyze.plugins.certificate_info._certificate_utils import extract_dns_subject_alternative_names
from sslyze.scanner.models import (
    CertificateInfoScanAttempt, CipherSuitesScanAttempt, SupportedEllipticCurvesScanAttempt
)

from model import data
from model.data import *
from model.data import CipherSuiteSupport
from monitoring import logs, metrics

logger = logs.get_logger(__name__)

trust_store_cache = {}

# def to_json(server_scan_result: ServerScanResult) -> str:
#     json_output = SslyzeOutputAsJson(
#         server_scan_results=[ServerScanResultAsJson.from_orm(result) for result in all_server_scan_results],
#         date_scans_started=date_scans_started,
#         date_scans_completed=datetime.utcnow(),
#     )

def process_server_scan_result(server_scan_result: ServerScanResult, message: dict) -> None:
    try:
        _process_server_scan_result(server_scan_result, message)
    except SQLAlchemyError as e:
        logger.warning(f"An SQLAlchemyError occurred during _process_server_scan_result: {e}. {traceback.format_exc()}")
        session: Session = data.get_session()
        # if we don't rollback now, the session will fail when trying to save the __next__ server_scan_result
        # and it will never recover from this problem
        # session handling seems severely broken?
        logger.info("SQLAlchemyError => rollback")
        session.rollback()
        logger.info("rollback complete")
        raise

def _process_server_scan_result(server_scan_result: ServerScanResult, message: dict) -> None:
    """
    Main function to process a ServerScanResult and write it into the database.

    :param server_scan_result: ServerScanResult to process
    :param message: dict containing the following keys: 'visitId', 'domainName'
    """

    session: Session = data.get_session()

    ssl_crawl_result = SslCrawlResult(
        visit_id=message['visitId'],
        hostname=message['domainName'],
        crawl_timestamp=datetime.datetime.utcnow(),
        domain_name=server_scan_result.server_location.hostname
    )

    # If the scan is not successful, write the error to the DB and exit
    if server_scan_result.scan_status == ServerScanStatusEnum.ERROR_NO_CONNECTIVITY:
        logger.info(f"Could not connect to {message}")
        ssl_crawl_result.ok = False
        ssl_crawl_result.problem = server_scan_result.connectivity_error_trace.__str__()
        session.add(ssl_crawl_result)
        session.commit()
        return

    if server_scan_result.scan_status == ServerScanStatusEnum.COMPLETED:
        ssl_crawl_result.ok = True
    else:
        ssl_crawl_result.ok = False
        logger.error("Unexpected value for scan_status: " + server_scan_result.scan_status)

    ssl_crawl_result.ip_address = server_scan_result.server_location.ip_address
    try:
        add_and_refresh(session, ssl_crawl_result)
    except IntegrityError:
        session.rollback()
        logger.warning(f"Duplicate result won't be saved for {message}")
        return

    cert_info_attempt = server_scan_result.scan_result.certificate_info

    # Certificate info
    get_cert_info_scan_result(cert_info_attempt, message, session, ssl_crawl_result)

    # Not so clean to list all the Cipher Suites scans but did not find a better way with SSLyze 5.0.0
    all_suite_scan_attempts = [
        server_scan_result.scan_result.ssl_2_0_cipher_suites,
        server_scan_result.scan_result.ssl_3_0_cipher_suites,
        server_scan_result.scan_result.tls_1_0_cipher_suites,
        server_scan_result.scan_result.tls_1_1_cipher_suites,
        server_scan_result.scan_result.tls_1_2_cipher_suites,
        server_scan_result.scan_result.tls_1_3_cipher_suites
    ]

    save_cipher_suites = config.get_env_bool("SSL_CRAWLER_SAVE_CIPHER_SUITES", default=False)
    save_curves        = config.get_env_bool("SSL_CRAWLER_SAVE_CURVES", default=False)

    # Cipher suites info
    start = time.time()
    for suite_scan_attempt in all_suite_scan_attempts:
        get_cipher_suite_scan_result(suite_scan_attempt, message, session, ssl_crawl_result, save_cipher_suites)
    end = time.time()
    metrics.duration_last_cipher_support.set(end - start)

    # Curve support info
    start = time.time()
    curve_scan_attempt = server_scan_result.scan_result.elliptic_curves
    get_curve_scan_result(curve_scan_attempt, message, session, ssl_crawl_result, save_curves)
    end = time.time()
    metrics.duration_last_curve_support.set(end - start)

    session.commit()
    return


def get_cert_info_scan_result(cert_info_attempt: CertificateInfoScanAttempt, message: dict, session: Session,
                              ssl_crawl_result: SslCrawlResult) -> None:
    """
    Get the result of a certificate info scan command and process it
    Modify in place the ssl_crawl_result if the scan was successful

    :param cert_info_attempt: CertificateInfoScanAttempt to process
    :param message: message received by the module containing the visitId and the domainName
    :param session: session to the database
    :param ssl_crawl_result: SslCrawlResult related to the command
    """

    if cert_info_attempt.status == ScanCommandAttemptStatusEnum.ERROR:
        logger.debug(f"An error occurred while retrieving the certificate info of {message}: "
                     f"{cert_info_attempt.error_reason}")

    elif cert_info_attempt.status == ScanCommandAttemptStatusEnum.COMPLETED:
        cert_info_result = cert_info_attempt.result
        ssl_crawl_result.hostname_used_for_server_name_indication = cert_info_result.hostname_used_for_server_name_indication
        ssl_crawl_result.nb_certificate_deployed = len(cert_info_result.certificate_deployments)

        process_cert_deployments(cert_info_result, ssl_crawl_result, session)


def get_cipher_suite_scan_result(suite_scan_attempt: CipherSuitesScanAttempt, message: dict, session: Session,
                                 ssl_crawl_result: SslCrawlResult,
                                 save_cipher_suites: bool = True
                                 ) -> None:
    """
    Get the result of a cipher suite scan command and process it
    Modify in place the ssl_crawl_result if the scan was successful

    :param suite_scan_attempt: CipherSuitesScanAttempt to process
    :param message: message received by the module containing the visitId and the domainName
    :param session: session to the database
    :param ssl_crawl_result: SslCrawlResult related to the command
    """

    if suite_scan_attempt.status == ScanCommandAttemptStatusEnum.ERROR:
        logger.warning(f"An error occurred while retrieving one of the cipher suite info of {message}: "
                       f"{suite_scan_attempt.error_reason}")
    elif suite_scan_attempt.status == ScanCommandAttemptStatusEnum.COMPLETED:
        suite_scan_result = suite_scan_attempt.result
        process_cipher_suites(suite_scan_result, ssl_crawl_result, session, save_cipher_suites)


def get_curve_scan_result(curve_scan_attempt: SupportedEllipticCurvesScanAttempt, message: dict, session: Session,
                          ssl_crawl_result: SslCrawlResult,
                          save_curves: bool = True) -> None:
    """
    Get the result of a supported curve scan command and process it
    Modify in place the ssl_crawl_result if the scan was successful

    :param curve_scan_attempt: SupportedEllipticCurvesScanAttempt to process
    :param message: message received by the module containing the visitId and the domainName
    :param session: session to the database
    :param ssl_crawl_result: SslCrawlResult related to the command
    """

    if curve_scan_attempt.status == ScanCommandAttemptStatusEnum.ERROR:
        logger.warning(f"An error occurred while retrieving the curve info of {message}: "
                       f"{curve_scan_attempt.error_reason}")
        ssl_crawl_result.problem = f"An error occurred while retrieving the curve info: {curve_scan_attempt.error_reason}"
    elif curve_scan_attempt.status == ScanCommandAttemptStatusEnum.COMPLETED:
        curve_scan_result = curve_scan_attempt.result
        ssl_crawl_result.support_ecdh_key_exchange = curve_scan_result.supports_ecdh_key_exchange
        process_curves(curve_scan_result, ssl_crawl_result, session, save_curves)


def process_curves(curve_scan_result: SupportedEllipticCurvesScanResult, ssl_crawl_result: SslCrawlResult,
                   session: Session, save_curves : bool = True) -> None:

    if not save_curves:
        logger.debug("Saving curve support is disabled")
        return

    if curve_scan_result.rejected_curves is not None and curve_scan_result.supported_curves is not None:

        to_save: List[CurveSupport] = list()
        for sslyze_curve in curve_scan_result.supported_curves:
            to_save.append(save_curve_and_support(sslyze_curve, ssl_crawl_result, session, True))

        for sslyze_curve in curve_scan_result.rejected_curves:
            to_save.append(save_curve_and_support(sslyze_curve, ssl_crawl_result, session, False))
        session.add_all(to_save)


def save_curve_and_support(sslyze_curve: EllipticCurve, ssl_crawl_result: SslCrawlResult, session: Session,
                           supported: bool) -> CurveSupport:
    curve = Curve(
        name=sslyze_curve.name,
        openssl_nid=sslyze_curve.openssl_nid
    )

    curve_id = add_or_get_curve(curve, session)

    curve_support = CurveSupport(
        ssl_crawl_result_id=ssl_crawl_result.id,
        curve_id=curve_id,
        supported=supported
    )
    return curve_support


def process_cipher_suites(suite_scan_result: CipherSuitesScanResult, ssl_crawl_result: SslCrawlResult,
                          session: Session,
                          save_cipher_suites: bool = True
                          ) -> None:
    # Check the version used for the current attempt
    if suite_scan_result.tls_version_used is TlsVersionEnum.SSL_2_0:
        ssl_crawl_result.support_ssl_2_0 = suite_scan_result.is_tls_version_supported
    elif suite_scan_result.tls_version_used is TlsVersionEnum.SSL_3_0:
        ssl_crawl_result.support_ssl_3_0 = suite_scan_result.is_tls_version_supported
    elif suite_scan_result.tls_version_used is TlsVersionEnum.TLS_1_0:
        ssl_crawl_result.support_tls_1_0 = suite_scan_result.is_tls_version_supported
    elif suite_scan_result.tls_version_used is TlsVersionEnum.TLS_1_1:
        ssl_crawl_result.support_tls_1_1 = suite_scan_result.is_tls_version_supported
    elif suite_scan_result.tls_version_used is TlsVersionEnum.TLS_1_2:
        ssl_crawl_result.support_tls_1_2 = suite_scan_result.is_tls_version_supported
    elif suite_scan_result.tls_version_used is TlsVersionEnum.TLS_1_3:
        ssl_crawl_result.support_tls_1_3 = suite_scan_result.is_tls_version_supported

    if save_cipher_suites:
        to_save: List[CipherSuiteSupport] = list()
        for accepted in suite_scan_result.accepted_cipher_suites:
            to_save.append(save_cipher_suite_and_support(suite_scan_result, ssl_crawl_result, session, accepted))
        for rejected in suite_scan_result.rejected_cipher_suites:
            to_save.append(save_cipher_suite_and_support(suite_scan_result, ssl_crawl_result, session, rejected))
        session.add_all(to_save)
    else:
        logger.debug("Saving cipher suites is disabled")


def save_cipher_suite_and_support(suite_scan_result: CipherSuitesScanResult, ssl_crawl_result: SslCrawlResult,
                                  session: Session,
                                  sslyze_cipher_suite: CipherSuiteAcceptedByServer | CipherSuiteRejectedByServer) -> CipherSuiteSupport:
    assert isinstance(sslyze_cipher_suite, (CipherSuiteAcceptedByServer, CipherSuiteRejectedByServer))

    cipher_suite = CipherSuite(
        iana_name=sslyze_cipher_suite.cipher_suite.name,
        openssl_name=sslyze_cipher_suite.cipher_suite.openssl_name
    )

    iana_name = add_or_get_cipher_suite(cipher_suite, session)

    cipher_suite_support = CipherSuiteSupport(
        ssl_crawl_result_id=ssl_crawl_result.id,
        cipher_suite_id=iana_name,
        protocol=suite_scan_result.tls_version_used.name,
        supported=isinstance(sslyze_cipher_suite, CipherSuiteAcceptedByServer)
    )
    return cipher_suite_support


def process_cert_deployments(cert_info_result: CertificateInfoScanResult, ssl_crawl_result: SslCrawlResult,
                             session: Session) -> None:
    for sslyze_deploy in cert_info_result.certificate_deployments:

        certs: List[Certificate] = list()

        if sslyze_deploy.verified_certificate_chain is not None:
            cert_chain = sslyze_deploy.verified_certificate_chain
        else:
            logger.debug(f"sslyze_deploy.verified_certificate_chain is None, using "
                         f"sslyze_deploy.received_certificate_chain: {sslyze_deploy.received_certificate_chain}")
            cert_chain = sslyze_deploy.received_certificate_chain

        for cert in cert_chain:

            certificate = Certificate(
                serial_number=str(cert.serial_number),
                public_key_schema=cert.public_key().__class__.__name__.replace("_", ""),
                public_key_length=getattr(cert.public_key(), "key_size", None),
                not_before=cert.not_valid_before,
                not_after=cert.not_valid_after,
                signature_hash_algorithm=getattr(cert.signature_hash_algorithm, "name", None),
                issuer=cert.issuer.rfc4514_string(),
                subject=cert.subject.rfc4514_string()[:500],
                version=cert.version.name,
                sha256_fingerprint=hashlib.sha256(cert.public_bytes(Encoding.DER)).hexdigest()
            )
            subject_alternative_names : List[str]
            subject_alternative_names = extract_dns_subject_alternative_names(cert)
            logger.debug(f"subject_alternative_names: {subject_alternative_names}")
            certificate.subject_alt_names = subject_alternative_names

            certs.append(certificate)

        previous_cert_sha256 = None
        certs.reverse()
        # Certs are now ordered from CA to leaf (0 -> end): easier to create the chain of trust
        for cert in certs:
            cert.signed_by_sha256 = previous_cert_sha256
            previous_cert_sha256 = add_or_get_certificate(cert, session)

        cert_deploy = CertificateDeployment(
            ssl_crawl_result_id=ssl_crawl_result.id,
            leaf_certificate_sha256=previous_cert_sha256,
            length_received_certificate_chain=len(sslyze_deploy.received_certificate_chain),
            leaf_certificate_subject_matches_hostname=sslyze_deploy.leaf_certificate_subject_matches_hostname,
            leaf_certificate_has_must_staple_extension=sslyze_deploy.leaf_certificate_has_must_staple_extension,
            leaf_certificate_is_ev=sslyze_deploy.leaf_certificate_is_ev,
            received_chain_contains_anchor_certificate=sslyze_deploy.received_chain_contains_anchor_certificate,
            received_chain_has_valid_order=sslyze_deploy.received_chain_has_valid_order,
            verified_chain_has_sha1_signature=sslyze_deploy.verified_chain_has_sha1_signature,
            verified_chain_has_legacy_symantec_anchor=sslyze_deploy.verified_chain_has_legacy_symantec_anchor,
            ocsp_response_is_trusted=sslyze_deploy.ocsp_response_is_trusted
        )

        add_and_refresh(session, cert_deploy)

        process_check_against_trust_stores(sslyze_deploy, cert_deploy, session)


def process_check_against_trust_stores(sslyze_deploy: CertificateDeploymentAnalysisResult,
                                       cert_deploy: CertificateDeployment, session: Session) -> None:
    for result in sslyze_deploy.path_validation_results:
        trust_store = TrustStore(
            name=result.trust_store.name,
            version=result.trust_store.version
        )

        trust_store_id = add_or_get_trust_store(trust_store, session)

        check_against_trust_store = CheckAgainstTrustStore(
            trust_store_id=trust_store_id,
            certificate_deployment_id=cert_deploy.id,
            valid=result.was_validation_successful,
        )

        add_and_refresh(session, check_against_trust_store)


def add_or_get_cipher_suite(cipher_suite: CipherSuite, session: Session) -> str:
    q = session.query(CipherSuite).filter_by(iana_name=cipher_suite.iana_name)
    if session.query(q.exists()).scalar():
        return cipher_suite.iana_name
    else:
        # If, in the meantime, the object is added by another thread in the DB we might get an IntegrityError
        # Therefore, we create a savepoint to rollback if needed and retry
        savepoint = session.begin_nested()
        try:
            session.add(cipher_suite)
            session.flush()
        except IntegrityError as error:
            logger.warning(f"{error} happened, retrying")
            savepoint.rollback()
            return add_or_get_cipher_suite(cipher_suite, session)
        else:
            session.refresh(cipher_suite)
            savepoint.commit()
            return cipher_suite.iana_name


def add_or_get_curve(curve: Curve, session: Session) -> int:
    curve_id = session.query(Curve.id).filter_by(openssl_nid=curve.openssl_nid).scalar()
    if curve_id is not None:
        return curve_id
    else:
        # If, in the meantime, the object is added by another thread in the DB we might get an IntegrityError
        # Therefore, we create a savepoint to rollback if needed and retry
        savepoint = session.begin_nested()
        try:
            session.add(curve)
            session.flush()
        except IntegrityError as error:
            logger.warning(f"{error} happened, retrying")
            savepoint.rollback()
            return add_or_get_curve(curve, session)
        else:
            session.refresh(curve)
            savepoint.commit()
            return curve.id


def add_or_get_certificate(certificate: Certificate, session: Session) -> str:

    cert_id = session.query(Certificate.sha256_fingerprint).filter_by(sha256_fingerprint=certificate.sha256_fingerprint).scalar()

    if certificate.subject_alt_names is not None and cert_id is not None and certificate.sha256_fingerprint is not None:
        cert = session.query(Certificate).filter_by(sha256_fingerprint=certificate.sha256_fingerprint).scalar()
        cert.subject_alt_names = certificate.subject_alt_names
        session.commit()
        session.flush()
        return cert_id

    if (certificate.subject_alt_names is None):
        cert_id = session.query(Certificate.sha256_fingerprint).filter_by(sha256_fingerprint=certificate.sha256_fingerprint).scalar()
    else:
        # we filter on subject_alt_names = None to force an update when cert in DB has no SAN's
        cert_id = session.query(Certificate.sha256_fingerprint).filter_by(sha256_fingerprint=certificate.sha256_fingerprint).filter_by(subject_alt_names = None).scalar()

    if cert_id is not None and certificate.sha256_fingerprint is not None:
        return cert_id
    else:
        # If, in the meantime, the object is added by another thread in the DB we might get an IntegrityError
        # Therefore, we create a savepoint to rollback if needed and retry
        savepoint = session.begin_nested()
        try:
            session.add(certificate)
            session.flush()
        except IntegrityError as error:
            logger.warning(f"{error} happened, retrying")
            savepoint.rollback()
            return add_or_get_certificate(certificate, session)
        else:
            session.refresh(certificate)
            savepoint.commit()
            return certificate.sha256_fingerprint


def add_or_get_trust_store(trust_store: TrustStore, session: Session) -> int:
    cache_key = f"{trust_store.name}:{trust_store.version}"
    found_trust_store_id = trust_store_cache.get(cache_key)
    if found_trust_store_id is not None:
        return found_trust_store_id

    logger.debug(f"Searching TrustStore in the DB with name = {trust_store.name} and version = {trust_store.version}")
    store_id = session.query(TrustStore.id).filter_by(name=trust_store.name, version=trust_store.version).scalar()
    if store_id is not None:
        trust_store_cache[cache_key] = store_id
        return store_id
    else:
        logger.debug(f"Saving the TrustStore in the DB with name = {trust_store.name} and version = {trust_store.version}")
        # If, in the meantime, the object is added by another thread in the DB we might get an IntegrityError
        # Therefore, we create a savepoint to rollback if needed and retry
        savepoint = session.begin_nested()
        try:
            session.add(trust_store)
            session.flush()
        except IntegrityError as error:
            logger.warning(f"{error} happened, retrying")
            savepoint.rollback()
            return add_or_get_trust_store(trust_store, session)
        else:
            session.refresh(trust_store)
            savepoint.commit()
            return trust_store.id


def add_and_refresh(session: Session, instance: Base) -> None:
    session.add(instance)
    session.flush()
    session.refresh(instance)
