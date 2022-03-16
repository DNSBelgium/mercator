import datetime
import random
import string
from uuid import uuid4

from sslyze import TlsVersionEnum

from custom_classes import DbTest
from mocking import get_mock_failed_scan_result, get_mock_server_scan_result, get_mock_cert_info_scan, \
    get_mock_cipher_suite_scan, get_mock_curve_scan
from model import data
from model.data import *
from process.save_result import get_cert_info_scan_result, process_server_scan_result, get_cipher_suite_scan_result, \
    get_curve_scan_result, add_or_get_cipher_suite, add_or_get_curve, add_or_get_certificate, add_or_get_trust_store

DOMAIN_NAME = 'test.be'
UUID = uuid4()
IP = '172.16.1.2'


def get_message() -> dict:
    return {'visitId': UUID, 'domainName': DOMAIN_NAME}


def get_random_message() -> dict:
    domain = ''.join((random.choice(string.ascii_lowercase) for _ in range(10))) + '.be'
    return {'visitId': uuid4(), 'domainName': domain}


class SaveResultTest(DbTest):

    def setUp(self) -> None:
        super().setUp()
        print("SaveResultTest::setUp")
        print(f"SaveResultTest::setUp => self.alembic_upgrade_done: {self.alembic_upgrade_done}")
        if not self.alembic_upgrade_done:
            raise Exception("Alembic upgrades should be finished first.")

    def tearDown(self) -> None:
        super().tearDown()

    def test_error_no_connectivity(self):

        print(f"test_error_no_connectivity => self.alembic_upgrade_done: {self.alembic_upgrade_done}")

        if not self.alembic_upgrade_done:
            raise Exception("Alembic upgrades should be finished first.")

        mock_scan_result = get_mock_failed_scan_result(DOMAIN_NAME)

        process_server_scan_result(mock_scan_result, get_message())

        session = data.get_session()
        result: SslCrawlResult = session.query(SslCrawlResult).first()

        self.assertEqual(DOMAIN_NAME, result.domain_name)
        self.assertFalse(result.ok)
        self.assertEqual(UUID, result.visit_id)

    def test_simple_server_scan(self):
        mock_scan_result = get_mock_server_scan_result(DOMAIN_NAME, IP)
        process_server_scan_result(mock_scan_result, get_message())

        session = data.get_session()
        result = session.query(SslCrawlResult).first()

        self.assertEqual(DOMAIN_NAME, result.domain_name)
        self.assertTrue(result.ok)

    def test_simple_server_scan_with_errors(self):
        mock_scan_result = get_mock_server_scan_result(DOMAIN_NAME, IP, "ERROR")
        process_server_scan_result(mock_scan_result, get_message())

        session = data.get_session()
        result = session.query(SslCrawlResult).first()

        self.assertEqual(DOMAIN_NAME, result.domain_name)
        self.assertTrue(result.ok)

    def test_cert_info_scan_result_one_self_signed(self):
        cert_info_mock = get_mock_cert_info_scan(DOMAIN_NAME, ['self-signed.pem'])

        session = data.get_session()
        ssl_crawl_result = SslCrawlResult(id=1, visit_id=UUID, domain_name=DOMAIN_NAME, hostname=DOMAIN_NAME,
                                          crawl_timestamp=datetime.datetime.utcnow(), ok=True)
        session.add(ssl_crawl_result)

        get_cert_info_scan_result(cert_info_mock, get_message(), session, ssl_crawl_result)
        session.commit()

        count = session.query(Certificate).count()
        self.assertEqual(1, count)

        certificate: Certificate = session.query(Certificate).first()
        self.assertEqual(4096, certificate.public_key_length)
        self.assertEqual(certificate.issuer, certificate.subject)
        self.assertEqual(None, certificate.signed_by_sha256)
        self.assertEqual('12214212603884544807', certificate.serial_number)
        self.assertGreater(certificate.not_after, certificate.not_before)

        cert_deploy: CertificateDeployment = session.query(CertificateDeployment).first()
        self.assertEqual(certificate.sha256_fingerprint, cert_deploy.leaf_certificate_sha256)
        self.assertEqual(1, cert_deploy.length_received_certificate_chain)
        self.assertTrue(cert_deploy.leaf_certificate_subject_matches_hostname)
        self.assertTrue(cert_deploy.leaf_certificate_has_must_staple_extension)
        self.assertTrue(cert_deploy.leaf_certificate_is_ev)
        self.assertTrue(cert_deploy.received_chain_contains_anchor_certificate)
        self.assertTrue(cert_deploy.received_chain_has_valid_order)
        self.assertTrue(cert_deploy.verified_chain_has_sha1_signature)
        self.assertTrue(cert_deploy.verified_chain_has_legacy_symantec_anchor)
        self.assertTrue(cert_deploy.ocsp_response_is_trusted)

    def test_cert_info_scan_result_with_chain(self):
        cert_chain = ['dnsbelgium-be-0.pem', 'dnsbelgium-be-1.pem', 'dnsbelgium-be-2.pem']
        cert_info_mock = get_mock_cert_info_scan(domain=DOMAIN_NAME, received_cert_chain=cert_chain[:-1],
                                                 verified_cert_chain=cert_chain)

        session = data.get_session()
        ssl_crawl_result = SslCrawlResult(id=1, visit_id=UUID, domain_name=DOMAIN_NAME, hostname=DOMAIN_NAME,
                                          crawl_timestamp=datetime.datetime.utcnow(), ok=True)
        session.add(ssl_crawl_result)

        get_cert_info_scan_result(cert_info_mock, get_message(), session, ssl_crawl_result)
        session.commit()

        cert_count = session.query(Certificate).count()
        self.assertEqual(len(cert_chain), cert_count)

        cert_deploy_count = session.query(CertificateDeployment).count()
        self.assertEqual(1, cert_deploy_count)

        cert_deploy: CertificateDeployment = session.query(CertificateDeployment).first()
        self.assertEqual(2, cert_deploy.length_received_certificate_chain)

        cert_sha256 = cert_deploy.leaf_certificate_sha256
        leaf: Certificate = session.query(Certificate).filter_by(sha256_fingerprint=cert_sha256).first()
        intermediate: Certificate = session.query(Certificate).filter_by(
            sha256_fingerprint=leaf.signed_by_sha256).first()
        root: Certificate = session.query(Certificate).filter_by(
            sha256_fingerprint=intermediate.signed_by_sha256).first()

        self.assertEqual(leaf.issuer, intermediate.subject)
        self.assertEqual(intermediate.issuer, root.subject)
        self.assertEqual(root.subject, root.issuer)
        self.assertIsNone(root.signed_by_sha256)

        self.assertEqual(2048, leaf.public_key_length)
        self.assertEqual('sha256', leaf.signature_hash_algorithm)

        self.assertEqual(2048, intermediate.public_key_length)
        self.assertEqual('sha256', intermediate.signature_hash_algorithm)

        self.assertEqual(2048, root.public_key_length)
        self.assertEqual('sha256', root.signature_hash_algorithm)

        self.assertEqual(3, len(leaf.subject_alt_names))
        self.assertListEqual(['dnsbelgium.be', 'production.dnsbelgium.be', 'www.dnsbelgium.be'], leaf.subject_alt_names)

    def test_cert_info_scan_result_with_trust_store(self):
        trust_stores = [
            {'name': 'trust_one', 'version': 'v1', 'success': True},
            {'name': 'trust_two', 'version': 'v2', 'success': False},
            {'name': 'trust_three', 'version': 'v3', 'success': True}
        ]

        cert_chain = ['self-signed.pem']

        cert_info_mock = get_mock_cert_info_scan(DOMAIN_NAME, received_cert_chain=cert_chain,
                                                 verified_cert_chain=cert_chain, trust_stores=trust_stores)

        session = data.get_session()
        ssl_crawl_result = SslCrawlResult(id=1, visit_id=UUID, domain_name=DOMAIN_NAME, hostname=DOMAIN_NAME,
                                          crawl_timestamp=datetime.datetime.utcnow(), ok=True)
        session.add(ssl_crawl_result)

        get_cert_info_scan_result(cert_info_mock, get_message(), session, ssl_crawl_result)
        session.commit()

        trust_count = session.query(TrustStore).count()
        self.assertEqual(len(trust_stores), trust_count)

        trust_check_count = session.query(CheckAgainstTrustStore).count()
        self.assertEqual(len(trust_stores), trust_check_count)

        for check in session.query(CheckAgainstTrustStore).all():
            trust_store = session.query(TrustStore).filter_by(id=check.trust_store_id).one()
            if trust_store.version == 'v2':
                self.assertFalse(check.valid)
            else:
                self.assertTrue(check.valid)

    def test_cipher_suite_scan_result(self):
        accepted = [
            {'name': 'first_accepted', 'openssl_name': 'openssl_first_accepted'},
            {'name': 'second_accepted', 'openssl_name': None},
        ]
        rejected = [
            {'name': 'first_rejected', 'openssl_name': 'openssl_first_rejected'}
        ]

        session = data.get_session()
        ssl_crawl_result = SslCrawlResult(id=1, visit_id=UUID, domain_name=DOMAIN_NAME, hostname=DOMAIN_NAME,
                                          crawl_timestamp=datetime.datetime.utcnow(), ok=True)
        session.add(ssl_crawl_result)
        all_tls_versions = [TlsVersionEnum.SSL_2_0, TlsVersionEnum.SSL_3_0, TlsVersionEnum.TLS_1_0,
                            TlsVersionEnum.TLS_1_1, TlsVersionEnum.TLS_1_2, TlsVersionEnum.TLS_1_3]
        for tls_version in all_tls_versions:
            suite_scan_attempt_mock = get_mock_cipher_suite_scan(accepted_ciphers=accepted, rejected_ciphers=rejected,
                                                                 tls_version=tls_version)
            get_cipher_suite_scan_result(suite_scan_attempt_mock, get_message(), session, ssl_crawl_result)

        session.commit()

        nb_cipher_suites = session.query(CipherSuite).count()
        self.assertEqual(len(accepted)+len(rejected), nb_cipher_suites)
        
        nb_supported = session.query(CipherSuiteSupport).filter_by(supported=True).count() 
        self.assertEqual(len(accepted)*len(all_tls_versions), nb_supported)
        
        nb_rejected = session.query(CipherSuiteSupport).filter_by(supported=False).count()
        self.assertEqual(len(rejected)*len(all_tls_versions), nb_rejected)

        ssl_crawl_result = session.query(SslCrawlResult).one()
        self.assertTrue(ssl_crawl_result.support_ssl_2_0)
        self.assertTrue(ssl_crawl_result.support_ssl_3_0)
        self.assertTrue(ssl_crawl_result.support_tls_1_0)
        self.assertTrue(ssl_crawl_result.support_tls_1_1)
        self.assertTrue(ssl_crawl_result.support_tls_1_2)
        self.assertTrue(ssl_crawl_result.support_tls_1_3)

    def test_curve_scan_result(self):
        accepted = [
            {'name': 'first_accepted', 'openssl_nid': 42},
            {'name': 'second_accepted', 'openssl_nid': 5},
        ]
        rejected = [
            {'name': 'first_rejected', 'openssl_nid': 8}
        ]

        session = data.get_session()
        ssl_crawl_result = SslCrawlResult(id=1, visit_id=UUID, domain_name=DOMAIN_NAME, hostname=DOMAIN_NAME,
                                          crawl_timestamp=datetime.datetime.utcnow(), ok=True)
        session.add(ssl_crawl_result)

        curve_scan_attempt_mock = get_mock_curve_scan(accepted_curves=accepted, rejected_curves=rejected)
        get_curve_scan_result(curve_scan_attempt_mock, get_message(), session, ssl_crawl_result)

        session.commit()

        nb_curves = session.query(Curve).count()
        self.assertEqual(len(accepted)+len(rejected), nb_curves)

        nb_supported = session.query(CurveSupport).filter_by(supported=True).count()
        self.assertEqual(len(accepted), nb_supported)

        nb_rejected = session.query(CurveSupport).filter_by(supported=False).count()
        self.assertEqual(len(rejected), nb_rejected)

        ssl_crawl_result = session.query(SslCrawlResult).one()
        self.assertTrue(ssl_crawl_result.support_ecdh_key_exchange)

    def test_add_or_get_cipher_suite(self):
        name = 'CUSTOM_IANA_NAME'
        cipher_suite_one = CipherSuite(iana_name=name)

        session = data.get_session()
        nb_cipher_suites = session.query(CipherSuite).count()
        self.assertEqual(0, nb_cipher_suites)

        add_or_get_cipher_suite(cipher_suite_one, session)

        nb_cipher_suites = session.query(CipherSuite).count()
        self.assertEqual(1, nb_cipher_suites)

        cipher_suite_two = CipherSuite(iana_name=name)

        add_or_get_cipher_suite(cipher_suite_two, session)

        nb_cipher_suites = session.query(CipherSuite).count()
        self.assertEqual(1, nb_cipher_suites)

    def test_add_or_get_curve(self):
        name = 'CUSTOM_CURVE_NAME'
        nid = 42
        curve_one = Curve(name=name, openssl_nid=nid)

        session = data.get_session()
        nb_curves = session.query(Curve).count()
        self.assertEqual(0, nb_curves)

        add_or_get_curve(curve_one, session)

        nb_curves = session.query(Curve).count()
        self.assertEqual(1, nb_curves)

        curve_two = Curve(name=name, openssl_nid=nid)

        add_or_get_curve(curve_two, session)

        nb_curves = session.query(Curve).count()
        self.assertEqual(1, nb_curves)

    def test_add_or_get_certificate(self):
        sha256sum = "9caeb25ba8e9ffac919cb1942ac60bda5b5be27499b976f7b49a3b7c9627a164"
        cert_one = Certificate(sha256_fingerprint=sha256sum)

        session = data.get_session()
        nb_certs = session.query(Certificate).count()
        self.assertEqual(0, nb_certs)

        output = add_or_get_certificate(cert_one, session)

        self.assertEqual(sha256sum, output)
        nb_certs = session.query(Certificate).count()
        self.assertEqual(1, nb_certs)

        cert_two = Certificate(sha256_fingerprint=sha256sum)

        output = add_or_get_certificate(cert_two, session)

        self.assertEqual(sha256sum, output)
        nb_certs = session.query(Certificate).count()
        self.assertEqual(1, nb_certs)

    def test_add_or_get_certificate_same_sn(self):
        sn = "0123456789"
        sha256sum1 = "9caeb25ba8e9ffac919cb1942ac60bda5b5be27499b976f7b49a3b7c9627a164"
        sha256sum2 = "5555555555555555555555555555555555555555555555555555555555555555"
        cert_one = Certificate(sha256_fingerprint=sha256sum1, serial_number=sn)

        session = data.get_session()
        nb_certs = session.query(Certificate).count()
        self.assertEqual(0, nb_certs)

        output = add_or_get_certificate(cert_one, session)

        self.assertEqual(sha256sum1, output)
        nb_certs = session.query(Certificate).count()
        self.assertEqual(1, nb_certs)

        cert_two = Certificate(sha256_fingerprint=sha256sum2, serial_number=sn)

        output = add_or_get_certificate(cert_two, session)

        self.assertEqual(sha256sum2, output)
        nb_certs = session.query(Certificate).count()
        self.assertEqual(2, nb_certs)

    def test_add_or_get_trust_store(self):
        name = "Trusted Store"
        version = "v00001"
        store_one = TrustStore(name=name, version=version)

        session = data.get_session()
        nb_stores = session.query(TrustStore).count()
        self.assertEqual(0, nb_stores)

        add_or_get_trust_store(store_one, session)

        nb_stores = session.query(TrustStore).count()
        self.assertEqual(1, nb_stores)

        store_two = TrustStore(name=name, version=version)

        add_or_get_trust_store(store_two, session)

        nb_stores = session.query(TrustStore).count()
        self.assertEqual(1, nb_stores)
