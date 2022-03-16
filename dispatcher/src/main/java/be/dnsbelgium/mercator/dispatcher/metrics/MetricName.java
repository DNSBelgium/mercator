package be.dnsbelgium.mercator.dispatcher.metrics;

public interface MetricName {

  String MESSAGES_OUT = "dispatcher.message.out";
  String MESSAGES_FAILED = "dispatcher.message.failed";
  String MESSAGES_IN = "dispatcher.message.in";
  String DUPLICATE_VISIT_IDS = "dispatcher.duplicate.visitids";

}
