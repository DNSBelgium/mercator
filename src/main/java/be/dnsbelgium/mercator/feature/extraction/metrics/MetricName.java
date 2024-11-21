package be.dnsbelgium.mercator.feature.extraction.metrics;

public interface MetricName {

  String COUNTER_INVALID_URL         = "feature.extraction.invalid.url";

  // let's count them so we can decide if we want to store these
  String COUNTER_SMS_LINK            = "feature.extraction.sms.link";
  String COUNTER_FILE_LINK           = "feature.extraction.file.link";
  String COUNTER_UNKNOWN_LINK        = "feature.extraction.unknown.link";

}
