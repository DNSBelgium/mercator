import datetime
from uuid import uuid4

from sqlalchemy.exc import IntegrityError

from custom_classes import DbTest
from model import data
from model.data import SslCrawlResult, Certificate, TrustStore, CertificateDeployment, Curve, CurveSupport, CipherSuite, \
    CipherSuiteSupport
from process.save_result import add_or_get_certificate


def get_ssl_crawl_result():
    uuid = uuid4()
    ssl_crawl_result = SslCrawlResult(
        visit_id=uuid,
        domain_name="test.be",
        hostname="test.be",
        crawl_timestamp=datetime.datetime.utcnow(),
        ok=True,
    )
    return ssl_crawl_result


class DataModelTest(DbTest):

    def setUp(self) -> None:
        super().setUp()
        print("DataModelTest::setUp")
        print(f"DataModelTest::setUp => self.alembic_upgrade_done: {self.alembic_upgrade_done}")
        if not self.alembic_upgrade_done:
            raise Exception("Alembic upgrades should be finished first.")

    def test_save_ssl_crawl_result(self):
        ssl_crawl_result = get_ssl_crawl_result()

        session = data.get_session()

        session.add(ssl_crawl_result)
        session.commit()

        result: SslCrawlResult = session.query(SslCrawlResult).first()

        self.assertTrue(result is not None)
        self.assertTrue(result.domain_name == "test.be")
        self.assertTrue(result.ok)

    def test_save_certificate(self):
        certificate = Certificate(
            version="v3",
            serial_number="0123456789",
            public_key_schema="RSA",
            public_key_length=1234,
            issuer="test1",
            subject="test2",
            signature_hash_algorithm="sha3",
            sha256_fingerprint="4a23ad59974fe2c6460b137260588e3c19321d28570f5e413c4bd157aa465912"
        )

        session = data.get_session()
        session.add(certificate)
        session.commit()

        result: Certificate = session.query(Certificate).filter_by(serial_number="0123456789").first()

        keys = ['version', 'serial_number', 'public_key_schema', 'public_key_length', 'issuer', 'subject',
                'signature_hash_algorithm']
        self.assertObjectsEqualByDictCasting(certificate, result, keys)

    def test_update_certificate(self):
        certificate = Certificate(
            version="v3",
            serial_number="0123456789",
            public_key_schema="RSA",
            public_key_length=1234,
            issuer="test1",
            subject="test2",
            signature_hash_algorithm="sha3",
            sha256_fingerprint="4a23ad59974fe2c6460b137260588e3c19321d28570f5e413c4bd157aa465912"
        )
        session = data.get_session()
        session.add(certificate)
        session.commit()

        result: Certificate = session.query(Certificate).filter_by(serial_number="0123456789").first()
        print(f'BEFORE: subject_alt_names: {result.subject_alt_names}')
        self.assertTrue(result.subject_alt_names is None)

        keys = ['version', 'serial_number', 'public_key_schema', 'public_key_length', 'issuer', 'subject',
                'signature_hash_algorithm']
        self.assertObjectsEqualByDictCasting(certificate, result, keys)

        certificate.subject_alt_names = ['dnsbelgium.be', 'production.dnsbelgium.be', 'www.dnsbelgium.be']
        #certificate.subject_alt_names = None
        add_or_get_certificate(certificate, session)

        session.commit()
        session.flush()

        result2: Certificate = session.query(Certificate).filter_by(serial_number="0123456789").first()
        print(f'AFTER: subject_alt_names: {result2.subject_alt_names}')

        rowsFound = session.query(Certificate).filter_by(serial_number="0123456789").count()
        print(f"rowsFound: {rowsFound}")

        self.assertFalse(result2.subject_alt_names is None)
        self.assertListEqual(['dnsbelgium.be', 'production.dnsbelgium.be', 'www.dnsbelgium.be'], result2.subject_alt_names)

    def test_save_trust_store(self):
        trust_store = TrustStore(
            name="My trust store",
            version="new",
        )

        session = data.get_session()
        session.add(trust_store)
        session.commit()

        result: TrustStore = session.query(TrustStore).first()

        keys = ["name", "version"]
        self.assertObjectsEqualByDictCasting(trust_store, result, keys)

    def test_unique_trust_store(self):
        trust_store = TrustStore(
            name="My trust store",
            version="new",
        )
        trust_store2 = TrustStore(
            name="My trust store",
            version="new",
        )

        session = data.get_session()
        session.add(trust_store)
        session.add(trust_store2)

        with self.assertRaises(IntegrityError):
            session.commit()

    def test_save_certificate_deployment(self):
        ssl_crawl_result = get_ssl_crawl_result()
        certificate = Certificate(
            serial_number="0123456789",
            sha256_fingerprint="9caeb25ba8e9ffac919cb1942ac60bda5b5be27499b976f7b49a3b7c9627a164"
        )

        session = data.get_session()
        session.add(ssl_crawl_result)
        session.add(certificate)
        session.flush()
        session.refresh(ssl_crawl_result)
        session.refresh(certificate)

        certificate_deployment = CertificateDeployment(
            ssl_crawl_result_id=ssl_crawl_result.id,
            leaf_certificate_sha256=certificate.sha256_fingerprint,
            length_received_certificate_chain=3,
            leaf_certificate_subject_matches_hostname=True,
            leaf_certificate_has_must_staple_extension=True,
            leaf_certificate_is_ev=True,
            received_chain_contains_anchor_certificate=True,
            received_chain_has_valid_order=True,
            verified_chain_has_sha1_signature=True,
            verified_chain_has_legacy_symantec_anchor=True,
            ocsp_response_is_trusted=True,
        )

        session.add(certificate_deployment)
        session.commit()

        result: CertificateDeployment = session.query(CertificateDeployment).first()

        keys = ['ssl_crawl_result_id', 'leaf_certificate_sha256', 'length_received_certificate_chain',
                'leaf_certificate_subject_matches_hostname', 'leaf_certificate_has_must_staple_extension',
                'leaf_certificate_is_ev', 'received_chain_contains_anchor_certificate',
                'received_chain_has_valid_order', 'verified_chain_has_sha1_signature',
                'verified_chain_has_legacy_symantec_anchor', 'ocsp_response_is_trusted']
        self.assertObjectsEqualByDictCasting(certificate_deployment, result, keys)

    def test_save_curve(self):
        curve = Curve(
            name="da curve",
            openssl_nid=42
        )

        session = data.get_session()
        session.add(curve)
        session.commit()

        result: Curve = session.query(Curve).first()
        keys = ["name", "openssl_nid"]
        self.assertObjectsEqualByDictCasting(curve, result, keys)

    def test_save_curve_support(self):
        curve = Curve(
            name="da curve",
            openssl_nid=42
        )

        ssl_crawl_result = get_ssl_crawl_result()

        session = data.get_session()
        session.add(curve)
        session.add(ssl_crawl_result)
        session.flush()
        session.refresh(curve)
        session.refresh(ssl_crawl_result)

        curve_support = CurveSupport(
            ssl_crawl_result_id=ssl_crawl_result.id,
            curve_id=curve.id,
            supported=True
        )

        session.add(curve_support)
        session.commit()

        result: CurveSupport = session.query(CurveSupport).first()
        keys = ["ssl_crawl_result_id", "curve_id", "supported"]
        self.assertObjectsEqualByDictCasting(curve_support, result, keys)

    def test_save_cipher_suite(self):
        cipher_suite = CipherSuite(
            iana_name="SUPER_CIPHER_SUITE"
        )

        session = data.get_session()
        session.add(cipher_suite)
        session.commit()

        result: Curve = session.query(CipherSuite).first()
        keys = ["iana_name"]
        self.assertObjectsEqualByDictCasting(cipher_suite, result, keys)

    def test_save_cipher_suite_support(self):
        cipher_suite = CipherSuite(
            iana_name="SUPER_CIPHER_SUITE"
        )
        ssl_crawl_result = get_ssl_crawl_result()

        session = data.get_session()
        session.add(cipher_suite)
        session.add(ssl_crawl_result)
        session.flush()
        session.refresh(cipher_suite)
        session.refresh(ssl_crawl_result)

        cipher_suite_support = CipherSuiteSupport(
            ssl_crawl_result_id=ssl_crawl_result.id,
            cipher_suite_id=cipher_suite.iana_name,
            protocol="TLS_1_3",
            supported=False,
        )

        session.add(cipher_suite_support)
        session.commit()

        result: CurveSupport = session.query(CipherSuiteSupport).first()
        keys = ["ssl_crawl_result_id", "cipher_suite_id", "protocol", "supported"]
        self.assertObjectsEqualByDictCasting(cipher_suite_support, result, keys)
