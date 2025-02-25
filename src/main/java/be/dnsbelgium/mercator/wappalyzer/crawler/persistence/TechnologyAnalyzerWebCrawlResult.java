package be.dnsbelgium.mercator.wappalyzer.crawler.persistence;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@Builder
@ToString
public class TechnologyAnalyzerWebCrawlResult {

  private String visitId;
  private String domainName;

  // detected technologies by the TechnologyAnalyzer
  private Set<String> detectedTechnologies;

}