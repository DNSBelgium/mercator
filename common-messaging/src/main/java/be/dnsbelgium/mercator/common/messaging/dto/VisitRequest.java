package be.dnsbelgium.mercator.common.messaging.dto;

import be.dnsbelgium.mercator.common.messaging.queue.QueueMessage;
import lombok.Value;

import java.util.UUID;

@Value
public class VisitRequest implements QueueMessage {

  UUID visitId;
  String domainName;

}
