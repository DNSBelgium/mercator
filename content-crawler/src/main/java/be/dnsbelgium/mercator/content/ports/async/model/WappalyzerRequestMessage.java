package be.dnsbelgium.mercator.content.ports.async.model;

import lombok.Data;

import java.util.UUID;

@Data
public class WappalyzerRequestMessage implements RequestMessage {

  private String url;
  private UUID visitId;
  private String domainName;

}
