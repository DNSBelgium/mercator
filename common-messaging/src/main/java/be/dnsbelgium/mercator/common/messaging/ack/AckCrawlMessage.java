package be.dnsbelgium.mercator.common.messaging.ack;

import be.dnsbelgium.mercator.common.messaging.queue.QueueMessage;
import lombok.Value;

import java.util.UUID;

@Value
public class AckCrawlMessage implements QueueMessage {

  UUID visitId;
  String domainName;
  CrawlerModule crawlerModule;

}
