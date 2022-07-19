package be.dnsbelgium.mercator.tls.metrics;

public interface MetricName {

  String COUNTER_VISITS_COMPLETED                   = "tls.crawler.visits.completed";
  String COUNTER_VISITS_FAILED                      = "tls.crawler.visits.failed";
  String COUNTER_DUPLICATE_VISITS                   = "tls.crawler.duplicate.visits";

  String COUNTER_SCANRESULT_CACHE_HITS               = "tls.crawler.scanresult.cache.hits";
  String COUNTER_SCANRESULT_CACHE_MISSES             = "tls.crawler.scanresult.cache.misses";
  String GAUGE_SCANRESULT_CACHE_SIZE                 = "tls.crawler.scanresult.cache.size";

}
