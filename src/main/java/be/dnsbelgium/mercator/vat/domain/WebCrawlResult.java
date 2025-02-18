package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@ToString(exclude = {"pageVisits", "htmlFeatures"})
public class WebCrawlResult {

  private String visitId;
  private String domainName;

  private String startUrl;
  private String matchingUrl;

  private Instant crawlStarted;
  private Instant crawlFinished;

  private List<String> vatValues;
  private List<String> visitedUrls;

  private List<PageVisit> pageVisits;
  private List<HtmlFeatures> htmlFeatures;

  public void abbreviateData() {
    domainName = StringUtils.abbreviate(domainName, 255);
    startUrl = StringUtils.abbreviate(startUrl, 255);
    matchingUrl = StringUtils.abbreviate(matchingUrl, 255);
  }
}
