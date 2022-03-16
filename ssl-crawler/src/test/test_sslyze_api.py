from datetime import datetime
import unittest
from uuid import uuid4

from sslyze import ServerScanRequest, ServerNetworkLocation, Scanner, ServerScanResult, ServerConnectivityStatusEnum, \
    TlsVersionEnum, ClientAuthRequirementEnum, ServerScanStatusEnum, ScanCommandAttemptStatusEnum

from process.utils import all_commands


class SslyzeApiTest(unittest.TestCase):
    """
    Those test cases mainly test SSLyze API.  Breaking changes in the API of SSLyze should cause those tests to fail.
    Those tests scan www.dnsbelgium.be and any configuration change to this website should cause those tests to fail.

    This test takes around 10 seconds to complete, but since most time is spent in setUpClass all test methods are
    reported as being very fast.
    """

    sslyze_result: ServerScanResult = None
    domain = 'dnsbelgium.be'
    ip = '45.60.74.42'
    uuid = uuid4()

    @classmethod
    def setUpClass(cls):

        print(f"SslyzeApiTest.setUpClass started at {datetime.now()}")
        print(f"class: {cls.__name__}")
        print(f"Scanning {cls.domain} at {cls.ip}")
        command = ServerScanRequest(server_location=ServerNetworkLocation(hostname=cls.domain, ip_address=cls.ip),
                                    scan_commands=all_commands, uuid=cls.uuid)

        scanner = Scanner()
        scanner.queue_scans([command])
        for result in scanner.get_results():
            if cls.sslyze_result is None:
                cls.sslyze_result = result
        print(f"SslyzeApiTest.setUpClass done at {datetime.now()}")
        print(f"cls.sslyze_result.scan_status: {cls.sslyze_result.scan_status}")

    def test_scan_status(self):
        self.assertEqual(ServerScanStatusEnum.COMPLETED, self.sslyze_result.scan_status)

    def test_uuid(self):
        self.assertEqual(self.uuid, self.sslyze_result.uuid)

    def test_server_location(self):
        server_location = self.sslyze_result.server_location
        self.assertEqual(self.domain, server_location.hostname)
        self.assertEqual(self.ip, server_location.ip_address)
        self.assertEqual(443, server_location.port)
        self.assertIsNone(server_location.http_proxy_settings)

    def test_network_configuration(self):
        network_configuration = self.sslyze_result.network_configuration
        self.assertEqual(self.domain, network_configuration.tls_server_name_indication)
        self.assertIsNone(network_configuration.tls_opportunistic_encryption)
        self.assertIsNone(network_configuration.tls_client_auth_credentials)
        self.assertIsNone(network_configuration.xmpp_to_hostname)

    def test_connectivity(self):
        connectivity_status = self.sslyze_result.connectivity_status
        self.assertEqual(ServerConnectivityStatusEnum.COMPLETED, connectivity_status)

        connectivity_error_trace = self.sslyze_result.connectivity_error_trace
        self.assertIsNone(connectivity_error_trace)

        connectivity_result = self.sslyze_result.connectivity_result
        self.assertEqual(TlsVersionEnum.TLS_1_3, connectivity_result.highest_tls_version_supported)
        self.assertEqual('TLS_AES_128_GCM_SHA256', connectivity_result.cipher_suite_supported)
        self.assertEqual(ClientAuthRequirementEnum.DISABLED, connectivity_result.client_auth_requirement)
        self.assertTrue(connectivity_result.supports_ecdh_key_exchange)

    def test_scan_certificate_info(self):
        certificate_info = self.sslyze_result.scan_result.certificate_info
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, certificate_info.status)
        self.assertIsNone(certificate_info.error_reason)
        self.assertIsNone(certificate_info.error_trace)

        result = certificate_info.result
        self.assertEqual(self.domain, result.hostname_used_for_server_name_indication)
        self.assertEqual(1, len(result.certificate_deployments))

        cert_deploy = result.certificate_deployments[0]
        self.assertEqual(2, len(cert_deploy.received_certificate_chain))
        self.assertEqual(3, cert_deploy.leaf_certificate_signed_certificate_timestamps_count)
        self.assertFalse(cert_deploy.leaf_certificate_has_must_staple_extension)
        self.assertFalse(cert_deploy.received_chain_contains_anchor_certificate)
        self.assertFalse(cert_deploy.verified_chain_has_legacy_symantec_anchor)
        self.assertFalse(cert_deploy.verified_chain_has_sha1_signature)
        self.assertIsNotNone(cert_deploy.ocsp_response)
        self.assertTrue(cert_deploy.leaf_certificate_is_ev)
        self.assertTrue(cert_deploy.leaf_certificate_subject_matches_hostname)
        self.assertTrue(cert_deploy.ocsp_response_is_trusted)
        self.assertTrue(cert_deploy.received_chain_has_valid_order)

        # Chain is trusted against all trust stores
        for path in cert_deploy.path_validation_results:
            self.assertTrue(path.was_validation_successful)

        leaf_cert = cert_deploy.received_certificate_chain[0]
        # Leaf certificate is currently valid (when the test is launched)
        self.assertTrue(leaf_cert.not_valid_before < datetime.utcnow())
        self.assertTrue(leaf_cert.not_valid_after > datetime.utcnow())

        self.assertEqual("CN=dnsbelgium.be,O=Domaine Name Registration België VZW,STREET=Philipssite 5  bus 13,"
                         "L=Leuven,ST=Vlaams-Brabant,C=BE,1.3.6.1.4.1.311.60.2.1.3=BE,2.5.4.5=0466.158.640,"
                         "2.5.4.15=Private Organization",
                         leaf_cert.subject.rfc4514_string())

    def test_scan_ssl_2_0_cipher_suites(self):
        ssl_2_0_cipher_suites = self.sslyze_result.scan_result.ssl_2_0_cipher_suites
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, ssl_2_0_cipher_suites.status)
        self.assertIsNone(ssl_2_0_cipher_suites.error_reason)
        self.assertIsNone(ssl_2_0_cipher_suites.error_trace)
        
        result = ssl_2_0_cipher_suites.result
        self.assertEqual(0, len(result.accepted_cipher_suites))
        self.assertGreater(len(result.rejected_cipher_suites), 0)
        self.assertFalse(result.is_tls_version_supported)

    def test_scan_ssl_3_0_cipher_suites(self):
        ssl_3_0_cipher_suites = self.sslyze_result.scan_result.ssl_3_0_cipher_suites
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, ssl_3_0_cipher_suites.status)
        self.assertIsNone(ssl_3_0_cipher_suites.error_reason)
        self.assertIsNone(ssl_3_0_cipher_suites.error_trace)

        result = ssl_3_0_cipher_suites.result
        self.assertEqual(0, len(result.accepted_cipher_suites))
        self.assertGreater(len(result.rejected_cipher_suites), 0)
        self.assertFalse(result.is_tls_version_supported)

    def test_scan_tls_1_0_cipher_suites(self):
        tls_1_0_cipher_suites = self.sslyze_result.scan_result.tls_1_0_cipher_suites
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, tls_1_0_cipher_suites.status)
        self.assertIsNone(tls_1_0_cipher_suites.error_reason)
        self.assertIsNone(tls_1_0_cipher_suites.error_trace)

        result = tls_1_0_cipher_suites.result
        self.assertEqual(0, len(result.accepted_cipher_suites))
        self.assertGreater(len(result.rejected_cipher_suites), 0)
        self.assertFalse(result.is_tls_version_supported)

    def test_scan_tls_1_1_cipher_suites(self):
        tls_1_1_cipher_suites = self.sslyze_result.scan_result.tls_1_1_cipher_suites
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, tls_1_1_cipher_suites.status)
        self.assertIsNone(tls_1_1_cipher_suites.error_reason)
        self.assertIsNone(tls_1_1_cipher_suites.error_trace)

        result = tls_1_1_cipher_suites.result
        self.assertEqual(0, len(result.accepted_cipher_suites))
        self.assertGreater(len(result.rejected_cipher_suites), 0)
        self.assertFalse(result.is_tls_version_supported)
        
    def test_scan_tls_1_2_cipher_suites(self):
        tls_1_2_cipher_suites = self.sslyze_result.scan_result.tls_1_2_cipher_suites
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, tls_1_2_cipher_suites.status)
        self.assertIsNone(tls_1_2_cipher_suites.error_reason)
        self.assertIsNone(tls_1_2_cipher_suites.error_trace)

        result = tls_1_2_cipher_suites.result
        self.assertGreater(len(result.accepted_cipher_suites), 0)
        self.assertGreater(len(result.rejected_cipher_suites), 0)
        self.assertTrue(result.is_tls_version_supported)
        
    def test_scan_tls_1_3_cipher_suites(self):
        tls_1_3_cipher_suites = self.sslyze_result.scan_result.tls_1_3_cipher_suites
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, tls_1_3_cipher_suites.status)
        self.assertIsNone(tls_1_3_cipher_suites.error_reason)
        self.assertIsNone(tls_1_3_cipher_suites.error_trace)

        result = tls_1_3_cipher_suites.result
        self.assertGreater(len(result.accepted_cipher_suites), 0)
        self.assertGreater(len(result.rejected_cipher_suites), 0)
        self.assertTrue(result.is_tls_version_supported)

    def test_scan_elliptic_curves(self):
        elliptic_curves = self.sslyze_result.scan_result.elliptic_curves
        self.assertEqual(ScanCommandAttemptStatusEnum.COMPLETED, elliptic_curves.status)
        self.assertIsNone(elliptic_curves.error_reason)
        self.assertIsNone(elliptic_curves.error_trace)

        result = elliptic_curves.result
        self.assertGreater(len(result.supported_curves), 0)
        self.assertGreater(len(result.rejected_curves), 0)
        self.assertTrue(result.supports_ecdh_key_exchange)
