package be.dnsbelgium.mercator.feature.extraction.metrics;

public interface MetricName {

  String COUNTER_VISITS_FAILED       = "feature.extraction.visits.failed";
  String COUNTER_VISITS_PROCESSED    = "feature.extraction.visits.processed";
  String COUNTER_VISITS_SKIPPED      = "feature.extraction.visits.skipped";
  String COUNTER_INVALID_URL         = "feature.extraction.invalid.url";

  // let's count them so we can decide if we want to store these
  String COUNTER_SMS_LINK            = "feature.extraction.sms.link";
  String COUNTER_FILE_LINK           = "feature.extraction.file.link";
  String COUNTER_UNKNOWN_LINK        = "feature.extraction.unknown.link";

  // when we are asked to process a (visit_it, url) combo that was already processed (and found out before the insert)
  String COUNTER_DUPLICATE_REQUESTS  = "feature.extraction.duplicate.requests";

  // when we are asked to process a (visit_it, url) combo that was already processed and the update property is true
  String COUNTER_UPDATE_REQUESTS     = "feature.extraction.update.requests";

  // when we processed a (visit_it, url) combo simultaneously and found out during the insert
  String COUNTER_DUPLICATE_KEYS      = "feature.extraction.duplicate.key.violations";

}
