package be.dnsbelgium.mercator.common.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@AllArgsConstructor
public class DispatcherRequest {

  UUID visitId;
  String domainName;
  List<String> labels;

}
