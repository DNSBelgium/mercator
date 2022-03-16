import json
from concurrent.futures import Future
from json import JSONDecodeError
from unittest.mock import patch, Mock
from uuid import uuid4

from sqlalchemy.exc import IntegrityError
from sslyze import ServerHostnameCouldNotBeResolved, ServerScanResult

from custom_classes import DbTest
from model.data import SslCrawlResult
from process.sslyze_scan import scan_domain, finalize

UUID = uuid4()
DOMAIN_NAME = 'test.be'


def get_message_mock(drop_visit_id=False) -> Mock:
    message_mock = Mock(specs=['body'])
    if drop_visit_id:
        message_mock.body = json.dumps({'domainName': DOMAIN_NAME})
    else:
        message_mock.body = json.dumps({'domainName': DOMAIN_NAME, 'visitId': str(UUID)})
    return message_mock


class ScanTest(DbTest):

    @patch('process.sslyze_scan.ServerScanRequest')
    def test_scan_invalid_hostname(self, server_scan_request_mock: Mock):
        server_scan_request_mock.side_effect = ServerHostnameCouldNotBeResolved
        message_mock = get_message_mock()

        message, result = scan_domain(message_mock)
        self.assertIsInstance(result, ServerHostnameCouldNotBeResolved)
        self.assertEqual(message_mock, message)

    @patch('process.sslyze_scan.json', specs=['loads'])
    def test_scan_error_json(self, json_mock: Mock):
        json_mock.loads.side_effect = JSONDecodeError('', '', 0)
        message_mock = get_message_mock()

        message, result = scan_domain(message_mock)
        self.assertIsInstance(result, ValueError)
        self.assertEqual(message_mock, message)

    def test_scan_error_assert(self):
        message_mock = get_message_mock(drop_visit_id=True)

        message, result = scan_domain(message_mock)
        self.assertIsInstance(result, AssertionError)
        self.assertEqual(message_mock, message)

    @patch('process.sslyze_scan.Scanner')
    @patch('process.sslyze_scan.ServerScanRequest')
    def test_scan_return_expected_result(self, _: Mock, scanner_mock: Mock):
        scanner_instance_mock = Mock(specs=['queue_scans', 'get_results'])
        scanner_mock.return_value = scanner_instance_mock
        scanner_instance_mock.get_results.return_value = iter(['The expected result', 'No this'])
        message_mock = get_message_mock()

        message, result = scan_domain(message_mock)

        self.assertEqual(message_mock, message)
        self.assertEqual('The expected result', result)

    @patch('process.sslyze_scan.get_session')
    def test_finalize_with_none_result(self, session_mock: Mock):
        message_mock = get_message_mock()
        future = Future()
        future.set_result((message_mock, None))

        finalize(future)

        session_mock.add.assert_not_called()

    @patch('process.sslyze_scan.get_queue')
    @patch('process.sslyze_scan.delete_message')
    @patch('process.sslyze_scan.send_message')
    @patch('process.sslyze_scan.get_session')
    def test_finalize_with_invalid_hostname(self, get_session_mock: Mock, send_message_mock: Mock,
                                            delete_message_mock: Mock, _: Mock):
        message_mock = get_message_mock()
        future = Future()
        future.set_result((message_mock, ServerHostnameCouldNotBeResolved()))
        session_mock = Mock()
        get_session_mock.return_value = session_mock

        finalize(future)

        session_mock.add.assert_called_once()
        saved: SslCrawlResult = session_mock.add.call_args.args[0]

        self.assertEqual(str(UUID), saved.visit_id)
        self.assertEqual(DOMAIN_NAME, saved.domain_name)
        self.assertFalse(saved.ok)

        send_message_mock.assert_called_once()
        delete_message_mock.assert_called_once()
        deleted_message = delete_message_mock.call_args.args[0]
        self.assertEqual(message_mock, deleted_message)

    @patch('process.sslyze_scan.get_queue')
    @patch('process.sslyze_scan.delete_message')
    @patch('process.sslyze_scan.send_message')
    def test_finalize_with_value_error(self, send_message_mock: Mock, delete_message_mock: Mock, _: Mock):
        message_mock = get_message_mock()
        future = Future()
        future.set_result((message_mock, ValueError()))

        finalize(future)

        send_message_mock.assert_called_once()
        delete_message_mock.assert_called_once()
        deleted_message = delete_message_mock.call_args.args[0]
        self.assertEqual(message_mock, deleted_message)

    @patch('process.sslyze_scan.get_queue')
    @patch('process.sslyze_scan.process_server_scan_result')
    @patch('process.sslyze_scan.delete_message')
    @patch('process.sslyze_scan.send_message')
    def test_finalize_with_expected_result(self, send_message_mock: Mock, delete_message_mock: Mock,
                                           process_server_scan_result_mock: Mock, _: Mock):
        message_mock = get_message_mock()
        future = Future()
        server_scan_result_mock = Mock(spec=ServerScanResult)
        future.set_result((message_mock, server_scan_result_mock))

        finalize(future)

        send_message_mock.assert_called_once()
        delete_message_mock.assert_called_once()
        deleted_message = delete_message_mock.call_args.args[0]
        self.assertEqual(message_mock, deleted_message)

        process_server_scan_result_mock.assert_called_once_with(server_scan_result_mock, json.loads(message_mock.body))

    def test_finalize_not_resolvable(self):
        message_mock = get_message_mock()
        future = Future()
        future.set_result((message_mock, ServerHostnameCouldNotBeResolved('Testing this error')))
        try:
            finalize(future)
        except IntegrityError:
            self.fail(f"finalize(future) raise IntegrityError unexpectedly")
