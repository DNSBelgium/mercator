import logging
from wsgiref.simple_server import WSGIRequestHandler

from pythonjsonlogger import jsonlogger

from utils import config

root_logger = logging.getLogger()
format_string = "%(asctime)%(name)%(levelname)%(funcName)%(lineno)%(message)%(pathname)%(module)%(levelno)%(thread)" \
                "%(threadName)"
test_format_string = "%(asctime)s %(levelname)s: %(message)s (%(name)s) [%(pathname)s:%(lineno)d]"

if config.get_env("SSL_CRAWLER_ENV") in ["test", "local"]:
    formatter = logging.Formatter(test_format_string)
else:
    formatter = jsonlogger.JsonFormatter(format_string)

console_handler = logging.StreamHandler()
console_handler.setLevel(logging.INFO)
console_handler.setFormatter(formatter)

root_logger.addHandler(console_handler)
root_logger.setLevel(logging.DEBUG)

main_logger = logging.getLogger("ssl_crawler")

if config.get_env("SSL_CRAWLER_ENV") in ["test", "local"]:
    main_logger.propagate = False
    main_console_handler = logging.StreamHandler()
    main_console_handler.setLevel(logging.DEBUG)
    main_console_handler.setFormatter(formatter)
    main_logger.addHandler(main_console_handler)
    main_logger.setLevel(logging.DEBUG)


def get_logger(name: str) -> logging.Logger:
    return logging.getLogger(f"{main_logger.name}.{name}")


class LoggingWSGIRequestHandler(WSGIRequestHandler):
    """
    Subclass of WSGIRequestHandler that uses the logger to write log messages
    """
    def log_message(self, format_param, *args):
        logger = logging.getLogger('prometheus_client_http')
        logger.debug(args[0], extra={'content_length': int(args[2]), 'response_code': int(args[1]),
                                     'address': self.address_string()})
