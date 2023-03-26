package be.dnsbelgium.mercator.common.messaging.dto;

import be.dnsbelgium.mercator.common.messaging.idn.IDN2008;
import be.dnsbelgium.mercator.common.messaging.queue.QueueMessage;
import lombok.Value;

import java.util.UUID;

@Value
public class VisitRequest implements QueueMessage {

  UUID visitId;
  String domainName;

  public String u_label() {
    return IDN2008.toUnicode(domainName);
  }

  public String a_label() {
    return IDN2008.toASCII(domainName);
  }

}
