package be.dnsbelgium.mercator.wappalyzer.crawler.persistence;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@ToString
public class TechnologyAnalyzerWebCrawlResult {

  private String visitId;
  private String domainName;

  private String startUrl;
  private String matchingUrl;

  private List<String> visitedUrls;

  // detected technologies by the TechnologyAnalyzer
  private Set<String> detectedTechnologies;

  public void abbreviateData() {
    domainName = StringUtils.abbreviate(domainName, 255);
    startUrl = StringUtils.abbreviate(startUrl, 255);
    matchingUrl = StringUtils.abbreviate(matchingUrl, 255);
  }
}