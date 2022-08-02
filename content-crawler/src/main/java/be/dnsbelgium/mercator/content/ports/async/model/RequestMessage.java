package be.dnsbelgium.mercator.content.ports.async.model;

import be.dnsbelgium.mercator.common.messaging.queue.QueueMessage;

import java.util.UUID;

public interface RequestMessage extends QueueMessage {

  void setVisitId(UUID visitId);
  void setDomainName(String domainName);
  void setUrl(String url);

}
