from concurrent.futures import ThreadPoolExecutor
from signal import signal, SIGINT, SIGTERM

from monitoring.logs import get_logger

logger = get_logger(__name__)


class SignalHandler:
    def __init__(self, executor: ThreadPoolExecutor):
        self.received_signal = False
        self.executor = executor
        signal(SIGINT, self._signal_handler)
        signal(SIGTERM, self._signal_handler)

    def _signal_handler(self, signal_received, frame):
        logger.info(f"Handling signal {signal_received}, exiting gracefully")
        self.received_signal = True
        self.executor.shutdown(cancel_futures=True, wait=False)
