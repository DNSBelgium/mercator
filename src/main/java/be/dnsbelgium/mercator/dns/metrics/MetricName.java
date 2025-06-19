package be.dnsbelgium.mercator.dns.metrics;

public interface MetricName {

  String COUNTER_VISITS_COMPLETED = "dns.crawler.visits.completed";
  String GEO_ENRICH = "dns.crawler.geo.enrich";

  String DNS_RESOLVER_LOOKUP_DONE = "dns.resolver.lookup.done";
  String DNS_RESOLVER_RECORDS_FOUND = "dns.resolver.records.found";
  String DNS_RESOLVER_CONCURRENT_CALLS = "dns.resolver.concurrent.calls";

}
