package be.dnsbelgium.mercator.common.messaging.dto;

import lombok.Value;

import java.util.UUID;

@Value
public class VisitRequest {

  UUID visitId;
  String domainName;

}
