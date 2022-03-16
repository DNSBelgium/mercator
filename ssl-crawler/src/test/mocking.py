import random
from unittest.mock import Mock

from cryptography import x509
from sslyze import (
    ServerScanStatusEnum, ScanCommandAttemptStatusEnum, TlsVersionEnum, CipherSuiteAcceptedByServer,
    CipherSuiteRejectedByServer
)


def load_file_content(filename, mode) -> bytes:
    with open('test/resources/' + filename, mode) as file:
        content = file.read()
    return bytes(content)


def get_mock_server_scan_result(domain, ip, status="NOT_SCHEDULED") -> Mock:
    server_scan_mock = Mock(name="Mock server scan result",
                            spec=['server_location', 'scan_status', 'scan_result'])

    server_scan_mock.server_location = Mock(spec=['hostname', 'ip_address'])
    server_scan_mock.server_location.hostname = domain
    server_scan_mock.server_location.ip_address = ip

    server_scan_mock.scan_status = ServerScanStatusEnum.COMPLETED

    server_scan_mock.scan_result = Mock(
        specs=['certificate_info', 'ssl_2_0_cipher_suites', 'ssl_3_0_cipher_suites', 'tls_1_0_cipher_suites',
               'tls_1_1_cipher_suites', 'tls_1_2_cipher_suites', 'tls_1_3_cipher_suites', 'elliptic_curves'])

    server_scan_mock.scan_result.certificate_info.status = status
    server_scan_mock.scan_result.ssl_2_0_cipher_suites.status = status
    server_scan_mock.scan_result.ssl_3_0_cipher_suites.status = status
    server_scan_mock.scan_result.tls_1_0_cipher_suites.status = status
    server_scan_mock.scan_result.tls_1_1_cipher_suites.status = status
    server_scan_mock.scan_result.tls_1_2_cipher_suites.status = status
    server_scan_mock.scan_result.tls_1_3_cipher_suites.status = status
    server_scan_mock.scan_result.elliptic_curves.status = status
    return server_scan_mock


def get_mock_failed_scan_result(domain) -> Mock:
    mock_scan_result = Mock(spec=['scan_status', 'server_location'])
    mock_scan_result.scan_status = ServerScanStatusEnum.ERROR_NO_CONNECTIVITY
    mock_scan_result.connectivity_error_trace = 'Mock stack trace'
    mock_scan_result.server_location.hostname = domain
    return mock_scan_result


def get_mock_cert_info_scan(domain: str, received_cert_chain: list, verified_cert_chain=None,
                            trust_stores=None) -> Mock:
    if trust_stores is None:
        trust_stores = list()
    cert_info_mock = Mock(specs=['status', 'result'])
    cert_info_mock.status = ScanCommandAttemptStatusEnum.COMPLETED
    cert_info_result_mock = Mock(specs=['hostname_used_for_server_name_indication', 'certificate_deployments'])
    cert_info_mock.result = cert_info_result_mock
    cert_info_result_mock.hostname_used_for_server_name_indication = domain
    cert_deploy_mock = Mock()
    cert_info_result_mock.certificate_deployments = [cert_deploy_mock]

    cert_deploy_mock.verified_certificate_chain = None if verified_cert_chain is None else [
        x509.load_pem_x509_certificate(load_file_content(filename, 'rb')) for filename in verified_cert_chain]

    cert_deploy_mock.received_certificate_chain = [
        x509.load_pem_x509_certificate(load_file_content(filename, 'rb')) for filename in received_cert_chain]

    cert_deploy_mock.path_validation_results = list()

    if trust_stores is not None:
        for store in trust_stores:
            validation = Mock(specs=['trust_store', 'was_validation_successful'])
            validation.trust_store.name = store['name']
            validation.trust_store.version = store['version']
            validation.was_validation_successful = store['success']
            cert_deploy_mock.path_validation_results.append(validation)

    cert_deploy_mock.leaf_certificate_subject_matches_hostname = True
    cert_deploy_mock.leaf_certificate_has_must_staple_extension = True
    cert_deploy_mock.leaf_certificate_is_ev = True
    cert_deploy_mock.received_chain_contains_anchor_certificate = True
    cert_deploy_mock.received_chain_has_valid_order = True
    cert_deploy_mock.verified_chain_has_sha1_signature = True
    cert_deploy_mock.verified_chain_has_legacy_symantec_anchor = True
    cert_deploy_mock.ocsp_response_is_trusted = True
    return cert_info_mock


def get_mock_cipher_suite_scan(accepted_ciphers: list, rejected_ciphers: list,
                               tls_version: TlsVersionEnum = None) -> Mock:
    suite_scan_attempt_mock = Mock(specs=['status', 'result'])
    suite_scan_attempt_mock.status = ScanCommandAttemptStatusEnum.COMPLETED

    suite_scan_result_mock = Mock(
        specs=['tls_version_used', 'is_tls_version_supported', 'accepted_cipher_suites', 'rejected_cipher_suites'])
    suite_scan_attempt_mock.result = suite_scan_result_mock

    suite_scan_result_mock.tls_version_used = tls_version if tls_version is not None else random.choice(
        [TlsVersionEnum.SSL_2_0, TlsVersionEnum.SSL_3_0, TlsVersionEnum.TLS_1_0, TlsVersionEnum.TLS_1_1,
         TlsVersionEnum.TLS_1_2, TlsVersionEnum.TLS_1_3])
    suite_scan_result_mock.is_tls_version_supported = len(accepted_ciphers) > 0

    suite_scan_result_mock.accepted_cipher_suites = list()
    for cipher in accepted_ciphers:
        cipher_mock = Mock(spec=CipherSuiteAcceptedByServer)
        cipher_suite_mock = Mock(specs=['name', 'openssl_name'])
        cipher_mock.cipher_suite = cipher_suite_mock
        cipher_suite_mock.name = cipher['name']
        cipher_suite_mock.openssl_name = cipher['openssl_name']
        suite_scan_result_mock.accepted_cipher_suites.append(cipher_mock)

    suite_scan_result_mock.rejected_cipher_suites = list()
    for cipher in rejected_ciphers:
        cipher_mock = Mock(spec=CipherSuiteRejectedByServer)
        cipher_suite_mock = Mock(specs=['name', 'openssl_name'])
        cipher_mock.cipher_suite = cipher_suite_mock
        cipher_suite_mock.name = cipher['name']
        cipher_suite_mock.openssl_name = cipher['openssl_name']
        suite_scan_result_mock.rejected_cipher_suites.append(cipher_mock)

    return suite_scan_attempt_mock


def get_mock_curve_scan(accepted_curves: list, rejected_curves: list) -> Mock:
    curve_scan_attempt_mock = Mock(specs=['status', 'result'])
    curve_scan_attempt_mock.status = ScanCommandAttemptStatusEnum.COMPLETED
    curve_scan_result_mock = Mock(specs=['supports_ecdh_key_exchange', 'supported_curves', 'rejected_curves'])

    curve_scan_attempt_mock.result = curve_scan_result_mock
    curve_scan_result_mock.supports_ecdh_key_exchange = True

    curve_scan_result_mock.supported_curves = list()
    for curve in accepted_curves:
        curve_mock = Mock(specs=['name', 'openssl_nid'])
        curve_mock.name = curve['name']
        curve_mock.openssl_nid = curve['openssl_nid']
        curve_scan_result_mock.supported_curves.append(curve_mock)

    curve_scan_result_mock.rejected_curves = list()
    for curve in rejected_curves:
        curve_mock = Mock(specs=['name', 'openssl_nid'])
        curve_mock.name = curve['name']
        curve_mock.openssl_nid = curve['openssl_nid']
        curve_scan_result_mock.rejected_curves.append(curve_mock)

    return curve_scan_attempt_mock
