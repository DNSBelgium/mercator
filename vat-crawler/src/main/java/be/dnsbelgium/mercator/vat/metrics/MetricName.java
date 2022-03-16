package be.dnsbelgium.mercator.vat.metrics;

public interface MetricName {

  String COUNTER_SUCCESS_VISITS                   = "vat.crawler.visits";
  String COUNTER_FAILED_VISITS                    = "vat.crawler.failures";
  String COUNTER_PAGES_FETCHED                    = "vat.crawler.pages.fetched";
  String COUNTER_OKHTTP_CACHE_HIT_COUNT           = "vat.crawler.okhttp.cache.hit.count";
  String COUNTER_OKHTTP_CACHE_HIT_RATIO           = "vat.crawler.okhttp.cache.hit.ratio";
  String COUNTER_OKHTTP_CACHE_NETWORK_COUNT       = "vat.crawler.okhttp.cache.network.count";
  String COUNTER_PAGES_FAILED                     = "vat.crawler.pages.failed";
  String COUNTER_PAGES_TOO_BIG                    = "vat.crawler.pages.too.big";
  String COUNTER_PAGES_CONTENT_TYPE_NOT_SUPPORTED = "vat.crawler.pages.content.not.supported";

  String COUNTER_SITES_WITH_VAT                   = "vat.crawler.sites.with.vat";
  String COUNTER_SITES_WITHOUT_VAT                = "vat.crawler.sites.without.vat";

  String TIMER_PAGE_RESPONSE_RECEIVED = "vat.crawler.page.response.received";
  String TIMER_PAGE_BODY_FETCHED      = "vat.crawler.page.body.fetched";

}
