package be.dnsbelgium.mercator.api.status;

import lombok.Data;

import java.util.UUID;

@Data
public class CrawlComponentStatus {

  private final UUID visit_id;
  private final boolean dns;
  private final boolean smtp;
  private final boolean muppets;
  private final boolean wappalyzer;

}
