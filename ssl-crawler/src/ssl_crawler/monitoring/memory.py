import logging
from monitoring import metrics
from monitoring.logs import get_logger
import threading
import psutil
import os

logger = get_logger(__name__)
assert logger.parent == logging.getLogger('ssl_crawler')

def mem_metrics():
    process = psutil.Process(os.getpid())
    bytes_memory = process.memory_info().rss
    MB = bytes_memory / (1024 * 1024)
    active_threads = threading.active_count()
    metrics.active_threads.set(active_threads)
    metrics.bytes_memory_used.set(bytes_memory)
    logger.info(f"Active threads {active_threads}. Memory used: {MB:.2f} MB")
