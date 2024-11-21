package be.dnsbelgium.mercator.smtp.metrics;

public interface MetricName {

  String TIMER_SMTP_ANALYSIS = "smtp.analyzer.per.domain";
  String SMTP_DOMAINS_DONE = "smtp.analyzer.domains.done";

  String COUNTER_CACHE_HITS = "smtp.analyzer.cache.hits";
  String COUNTER_CACHE_MISSES = "smtp.analyzer.cache.misses";
  String GAUGE_CACHE_SIZE = "smtp.analyzer.cache.size";

  String TIMER_IP_CRAWL = "smtp.analyzer.ip.timer";
  String COUNTER_CONVERSATION_FAILED = "smtp.analyzer.conversation.failed";
  String COUNTER_INVALID_HOSTNAME = "smtp.analyzer.invalid_hostname.counter";
  String COUNTER_NETWORK_ERROR = "smtp.analyzer.network.error.counter";

  // number of times we could not find MX records for a domain
  String COUNTER_NO_MX_RECORDS_FOUND = "smtp.analyzer.no.mxrecords.found.counter";

  // The time needed to connect and receive initial banner
  String TIMER_SMTP_CONNECT = "smtp.analyzer.smtp.connect";

  // The time between sending EHLO and receiving a response
  String TIMER_EHLO_RESPONSE_RECEIVED = "smtp.analyzer.ehlo.response.received";

  // The time between sending STARTTLS and receiving a response
  String TIMER_STARTTLS_COMPLETED = "smtp.analyzer.starttls.completed";

  // The time between sending QUIT and receiving a response
  String TIMER_SESSION_QUIT = "smtp.analyzer.session.closed";

  // the time spent to handle an exception during an SMTP conversation
  String TIMER_HANDLE_CONVERSATION_EXCEPTION = "smtp.analyzer.handle.conversation.exception";

  // number of IPs that were skipped (because IPv6, loopback, ...)
  String COUNTER_ADDRESSES_SKIPPED = "smtp.analyzer.addresses.skipped";
}
