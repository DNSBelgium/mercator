from prometheus_client import Counter, Gauge

number_of_requests_processed = Counter('ssl_crawler_completed_scans_counter', 'Number of incoming requests successfully processed by the module')
number_of_unresolved_domains = Counter('ssl_crawler_unresolved_domains_counter', 'Number of incoming requests with an unresolvable domain')
unknown_errors = Counter('ssl_crawler_unknown_errors_counter', 'Number of failed for an unknown reason')
deserializing_errors = Counter('ssl_crawler_deserializing_errors_counter', 'Number errors while deserializing the message body')
empty_callback = Counter('ssl_crawler_empty_callback_error_counter', 'Number callback that were empty after the scan')
already_saved = Counter('ssl_crawler_scans_already_saved', 'Number of scans that were skipped because they were already in the DB')

queue_size = Gauge("ssl_crawler_queue_size", "Number of jobs in the queue of the ThreadPoolExecutor")
ideal_queue_size = Gauge("ssl_crawler_ideal_queue_size", "Approximate number of jobs to have in the queue")
active_threads = Gauge("ssl_crawler_active_threads", "Number of active threads")
bytes_memory_used = Gauge("ssl_crawler_bytes_memory_used", "Memory used (in bytes)")
duration_last_scan = Gauge("ssl_crawler_duration_last_scan", "Duration (in seconds) of last SSL scan")
duration_last_cipher_support = Gauge("ssl_crawler_duration_last_cipher_support_insert", "Duration (in seconds) of the last cipher suite support insert")
duration_last_curve_support = Gauge("ssl_crawler_duration_last_curve_support_insert", "Duration (in seconds) of the last curve support insert")
