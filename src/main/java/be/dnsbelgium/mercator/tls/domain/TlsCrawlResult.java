package be.dnsbelgium.mercator.tls.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TlsCrawlResult {

  private final String visitId;

  private final String domainName;

  private final Instant crawlTimestamp;

  private final List<TlsVisit> visits;

  public TlsCrawlResult(String visitId, String domainName, List<TlsVisit> visits, Instant crawlTimestamp) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.visits = visits;
    this.crawlTimestamp = crawlTimestamp;
  }
}
