package be.dnsbelgium.mercator.common.messaging.ack;

import java.util.Arrays;

public enum CrawlerModule {

  DNS(),
  MUPPETS(),
  WAPPALYZER(),
  SMTP(),
  VAT(),
  TLS(),
  // we need this enum value as long as there corresponding rows in dispatcher.dispatcher_event_acks
  SSL(false),
  ;

  // this allows us to already use a new enum value in the code without having to deploy the corresponding module
  // Otherwise Dispatcher would stop notifying about finished crawls.
  private final boolean enabled;

  CrawlerModule() {
    this(true);
  }

  CrawlerModule(boolean enabled) {
    this.enabled = enabled;
  }

  public static long numberOfEnabledModules() {
    return Arrays.stream(CrawlerModule.values()).filter(CrawlerModule::isEnabled).count();
  }

  public boolean isEnabled() {
    return enabled;
  }
}
