package be.dnsbelgium.mercator.web.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"pageVisits"})
@EqualsAndHashCode
@JsonIgnoreProperties(value = { "tld" })
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

  @JsonIgnore
  public int year() {
    if (crawlStarted != null) {
      ZonedDateTime dateTime = crawlStarted.atZone(UTC);
      return dateTime.getYear();
    }
    return 0;
  }

  @JsonIgnore
  public int month() {
    if (crawlStarted != null) {
      ZonedDateTime dateTime = crawlStarted.atZone(UTC);
      return dateTime.getMonthValue();
    }
    return 0;
  }

}
