package be.dnsbelgium.mercator.content.ports.async.model;

import java.util.UUID;

public interface RequestMessage {

  void setVisitId(UUID visitId);
  void setDomainName(String domainName);
  void setUrl(String url);

}
