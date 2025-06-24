package be.dnsbelgium.mercator.vat.domain;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static be.dnsbelgium.mercator.common.SurrogateCodePoints.removeIncompleteSurrogates;

@Builder
@AllArgsConstructor
@Getter
@ToString
@NoArgsConstructor(force = true)
@EqualsAndHashCode
public class PageVisit {

  Instant crawlStarted;
  Instant crawlFinished;

  private final Integer statusCode;

  private final String url;

  private String linkText;

  private final String path;

  /** The raw content found at the URL above. Can be TXT or HTML or XML or ... */
  private String responseBody;

  private final Map<String, List<String>> headers;

  private final Long contentLength;

  private final String finalUrl;

  @Builder.Default
  private List<String> vatValues = new ArrayList<>();

  public PageVisit(
      String url,
      String finalUrl,
      String path,
      Instant crawlStarted,
      Instant crawlFinished,
      int statusCode,
      String responseBody,
      List<String> vatValues,
      long contentLength,
      Map<String, List<String>> headers) {
    this.url  = cleanUp(url, 500);
    this.finalUrl = cleanUp(finalUrl, 500);
    this.path = cleanUp(path,500);
    this.crawlStarted = crawlStarted;
    this.crawlFinished = crawlFinished;
    this.statusCode = statusCode;
    this.responseBody = cleanUp(responseBody, 20_000);
    this.vatValues = vatValues;
    // 90% of the .be websites have a html length below 281.187 bytes
    // 61k .be websites have (on the landing page) a html document over 500.000 bytes
    // Their combined html sums up to 50 GB (average of 872k) so we would 'save' 32 GB by truncating to 500k bytes
    // The 1.3 million other websites sum up to 89 GB
    this.contentLength = contentLength;
    this.headers = headers;
  }

  private String cleanUp(String input, int maxLength) {
    if (input == null) {
      return null;
    }
    if (input.contains("\u0000")) {
      return null;
    }
    String abbreviated = StringUtils.abbreviate(input, maxLength);
    // we need to remove incomplete surrogate code points
    // to avoid JSON files that duckdb (and jq) cannot read
    return removeIncompleteSurrogates(abbreviated);
  }

  public void setLinkText(String linkText) {
    this.linkText  = cleanUp(linkText, 500);
  }

}
