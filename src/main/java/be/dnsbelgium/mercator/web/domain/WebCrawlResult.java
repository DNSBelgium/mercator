package be.dnsbelgium.mercator.web.domain;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"pageVisits"})
@EqualsAndHashCode
public class WebCrawlResult {

  private String visitId;
  private String domainName;

  private String matchingUrl;

  private Instant crawlStarted;
  private Instant crawlFinished;

  private List<String> vatValues;
  private List<String> visitedUrls;

  private List<PageVisit> pageVisits;

  public void abbreviateData() {
    domainName = StringUtils.abbreviate(domainName, 255);
    matchingUrl = StringUtils.abbreviate(matchingUrl, 255);
  }
}
