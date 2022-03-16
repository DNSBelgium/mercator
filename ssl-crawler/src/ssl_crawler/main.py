import concurrent.futures
import logging
import ssl
import threading
import time
from wsgiref.simple_server import make_server

from alembic.config import Config
from prometheus_client import make_wsgi_app

from alembic import command
from model import data
from monitoring import metrics, logs, memory
from process.sslyze_scan import scan_domain, finalize
from sqs_messaging.message import receive_messages
from sqs_messaging.queue import get_queue
from utils import config
from utils.signal_handler import SignalHandler

if __name__ == "__main__":

    logger = logging.getLogger("ssl_crawler")

    save_cipher_suites = config.get_env_bool("SSL_CRAWLER_SAVE_CIPHER_SUITES", default=False)
    save_curves = config.get_env_bool("SSL_CRAWLER_SAVE_CURVES", default=False)

    logger.info(f"SSL_CRAWLER_SAVE_CIPHER_SUITES = {save_cipher_suites}")
    logger.info(f"SSL_CRAWLER_SAVE_CURVES        = {save_curves}")

    # Update database definition to the latest version
    alembic_cfg = Config("alembic.ini")
    alembic_cfg.set_section_option('alembic', 'sqlalchemy.url', config.get_db_string())
    command.upgrade(alembic_cfg, "head")

    input_queue = get_queue(config.get_env('SSL_CRAWLER_INPUT_QUEUE'))

    # TODO connect to DB using SSL
    port = int(config.get_env('SERVER_PORT'))
    logger.info(f"Starting server on port {port}")

    app = make_wsgi_app()
    httpd = make_server(config.get_env('SERVER_BIND_ADDRESS', '0.0.0.0'), port, app,
                        handler_class=logs.LoggingWSGIRequestHandler)

    httpd.socket = ssl.wrap_socket(httpd.socket, server_side=True, certfile=config.get_env('CERTIFICATE_FILE'),
                                   ssl_version=ssl.PROTOCOL_TLSv1_2)
    t = threading.Thread(target=httpd.serve_forever)
    t.daemon = True
    t.start()

    executor = concurrent.futures.ThreadPoolExecutor(max_workers=int(config.get_env('SSL_CRAWLER_MAX_WORKERS')))
    signal_handler = SignalHandler(executor)
    backoff = 1
    ideal_size = executor._max_workers * 2

    while not signal_handler.received_signal:
        queue_size = executor._work_queue.qsize()

        metrics.queue_size.set(queue_size)
        metrics.ideal_queue_size.set(ideal_size)
        memory.mem_metrics()

        if queue_size < ideal_size:
            messages = receive_messages(input_queue, int(config.get_env('SSL_CRAWLER_BATCH_SIZE')), 10)
            try:
                for message in messages:
                    if not signal_handler.received_signal:
                        executor.submit(scan_domain, message=message).add_done_callback(finalize)
            except Exception as e:
                logger.error(f"Main loop: {e}", extra=e.__dict__)

        else:
            logger.debug(f"Backoff for {backoff} sec")
            time.sleep(backoff)

    data.close_session()
