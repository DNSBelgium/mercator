from __future__ import annotations

import datetime
import json
import logging
import time
import traceback
from concurrent.futures import Future

from botocore.exceptions import ClientError
from sslyze import ServerScanRequest, ServerNetworkLocation, ServerHostnameCouldNotBeResolved, Scanner, ServerScanResult

from model import data
from model.data import SslCrawlResult, get_session
from monitoring import metrics
from monitoring.logs import get_logger
from process.save_result import process_server_scan_result
from process.utils import all_commands
from sqs_messaging.message import send_message, delete_message
from sqs_messaging.queue import get_queue, sqs
from utils.config import get_env

logger = get_logger(__name__)
assert logger.parent == logging.getLogger('ssl_crawler')

queue_dict = {}

domains_scanned = 0

def scan_domain(message: sqs.Message) -> (sqs.Message, ServerScanResult | Exception | None):
    """
    Scans the domain contained in the body of the message.  The return value is then used by finalize(future: Future)

    :param message: sqs.Message containing the visit request
    :return: tuple containing the message and one of the following: the result, an Exception or None
    """
    try:
        body = json.loads(message.body)
        assert "domainName" in body
        assert "visitId" in body
    except AssertionError as error:
        logger.warning(f"Could not find all the fields in {message.body}: {error}")
        return message, error
    except ValueError as error:
        logger.warning(f"Error while deserializing {message.body}")
        return message, error

    session = data.get_session()
    q = session.query(SslCrawlResult).filter_by(visit_id=body["visitId"], hostname=body["domainName"])
    if session.query(q.exists()).scalar():
        logger.warning(f"Skipping scan for {message.body}, already in the database")
        metrics.already_saved.inc()
        return message, AttributeError('An ssl_crawl_result already exists for the given values')

    try:
        domain = body["domainName"]
        logger.info(f"Queuing scan for {domain}")
        command = ServerScanRequest(server_location=ServerNetworkLocation(hostname=domain), scan_commands=all_commands)
    except ServerHostnameCouldNotBeResolved as error:
        logger.info(f"Error resolving the supplied hostname: {body}")
        return message, error

    start = time.time()

    # default is 10 concurrent_server_scans and 5 connections per server, resulting in 50 threads being created
    scanner = Scanner(concurrent_server_scans_limit=1, per_server_concurrent_connections_limit=5)

    scanner.queue_scans([command])

    server_scan_result = None
    for result in scanner.get_results():
        if server_scan_result is None:
            server_scan_result = result

    del scanner
    del command

    done = time.time()
    seconds = done - start
    metrics.duration_last_scan.set(seconds)
    global domains_scanned
    domains_scanned = domains_scanned + 1
    logger.info(f"Scanned {domain} in {seconds:.2f} seconds")
    logger.debug(f"Total domains_scanned {domains_scanned}")
    return message, server_scan_result


def finalize(future: Future) -> None:
    """
    Callback function triggered after scan_domain(message: sqs.Message).
    Handle the eventual errors from the previous function and launch the processing of the result

    :param future: Future containing the return value of the scan_domain(message: sqs.Message) function
    """

    message: sqs.Message
    result: ServerScanResult
    message, result = future.result()

    message_body: dict = json.loads(message.body)

    if result is None:
        logger.warning(f"Received an empty result for the callback")
        metrics.empty_callback.inc()
        return

    if isinstance(result, ServerHostnameCouldNotBeResolved):
        session = get_session()
        ssl_crawl_result = SslCrawlResult(
            visit_id=message_body['visitId'],
            hostname=message_body['domainName'],
            crawl_timestamp=datetime.datetime.utcnow(),
            domain_name=message_body['domainName'],
            ok=False,
            problem=result.__str__()
        )

        session.add(ssl_crawl_result)
        session.commit()

        try:
            ack_and_delete(message, message_body)
            metrics.number_of_unresolved_domains.inc()
        # We return anyway just after that call so the Exception can be ignored
        except ClientError:
            pass
        return

    if isinstance(result, AttributeError):
        try:
            ack_and_delete(message, message_body)
        # We return anyway just after that call so the Exception can be ignored
        except ClientError:
            pass
        return

    if isinstance(result, (AssertionError, ValueError)):
        # TODO: should we delete messages that could not be decoded or leave them on the queue?
        #  Currently, they are deleted and the failure counter is not incremented
        try:
            ack_and_delete(message, message_body)
            metrics.deserializing_errors.inc()
        # We return anyway just after that call so the Exception can be ignored
        except ClientError:
            pass
        return

    # Insert data into the DB
    try:
        process_server_scan_result(result, message_body)
    except Exception as e:
        logger.warning(f"An error occurred while saving the data: {e}. {traceback.format_exc()}")
        metrics.unknown_errors.inc()
        return

    try:
        ack_and_delete(message, message_body)
    except ClientError:
        return

    logger.debug(f"Done for {message_body}")
    metrics.number_of_requests_processed.inc()


def get_output_queue():
    output_queue = queue_dict.get("SSL_CRAWLER_OUTPUT_QUEUE")
    if output_queue is None:
        logger.info("Looking up SSL_CRAWLER_OUTPUT_QUEUE")
        output_queue = get_queue(get_env('SSL_CRAWLER_OUTPUT_QUEUE'))
        queue_dict["SSL_CRAWLER_OUTPUT_QUEUE"] = output_queue
    return output_queue

def ack_and_delete(message: sqs.Message, message_body: dict) -> None:
    # Send ACK
    try:
        domain = message_body['domainName']
        visit_id = message_body['visitId']
        output_queue = get_output_queue()
        send_message(output_queue, json.dumps({'visitId': visit_id, 'domainName': domain, 'crawlerModule': 'SSL'}))
        logger.debug(f"Ack for {domain}")
    except ClientError as e:
        logger.warning(f"Could not ACK message {message.body} because {e}")
        metrics.unknown_errors.inc()
        raise e

    # Delete message from input queue
    try:
        delete_message(message)
        logger.debug(f"Deleted {message.body}")
    except ClientError as e:
        logger.warning(f"Could not delete message {message.body} because {e}")
        metrics.unknown_errors.inc()
        raise e
