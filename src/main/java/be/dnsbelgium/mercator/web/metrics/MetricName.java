package be.dnsbelgium.mercator.web.metrics;

public interface MetricName {

  String COUNTER_PAGES_FETCHED                    = "web.crawler.pages.fetched";
  String COUNTER_OKHTTP_CACHE_SIZE                = "web.crawler.okhttp.cache.size.bytes";
  String COUNTER_OKHTTP_CACHE_HIT_COUNT           = "web.crawler.okhttp.cache.hit.count";
  String COUNTER_OKHTTP_CACHE_HIT_RATIO           = "web.crawler.okhttp.cache.hit.ratio";
  String COUNTER_OKHTTP_CACHE_NETWORK_COUNT       = "web.crawler.okhttp.cache.network.count";
  String COUNTER_PAGES_FAILED                     = "web.crawler.pages.failed";
  String COUNTER_PAGES_TOO_BIG                    = "web.crawler.pages.too.big";
  String COUNTER_PAGES_CONTENT_TYPE_NOT_SUPPORTED = "web.crawler.pages.content.not.supported";

  String COUNTER_SITES_WITH_VAT                   = "web.crawler.sites.with.vat";
  String COUNTER_SITES_WITHOUT_VAT                = "web.crawler.sites.without.vat";

  String TIMER_PAGE_RESPONSE_RECEIVED = "web.crawler.page.response.received";
  String TIMER_PAGE_BODY_FETCHED      = "web.crawler.page.body.fetched";

  String COUNTER_WEB_CRAWLS_DONE = "web.crawls.done";
}
