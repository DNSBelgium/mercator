package be.dnsbelgium.mercator.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class SearchVisitIdResultItem {
  private String visitId;
  private Instant timestamp;

  public SearchVisitIdResultItem(String visitId, Instant timestamp) {
    this.visitId = visitId;
    this.timestamp = timestamp;
  }

}
