package be.dnsbelgium.mercator.common.messaging.ack;

import lombok.Value;

import java.util.UUID;

@Value
public class AckCrawlMessage {

  UUID visitId;
  String domainName;
  CrawlerModule crawlerModule;

}
