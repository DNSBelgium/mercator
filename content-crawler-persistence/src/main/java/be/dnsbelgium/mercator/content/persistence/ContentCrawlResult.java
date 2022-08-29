package be.dnsbelgium.mercator.content.persistence;

import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "content_crawl_result")
public class ContentCrawlResult extends AbstractAggregateRoot<ContentCrawlResult> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "visit_id")        private UUID          visitId;
  @Column(name = "domain_name")     private String        domainName;
  @Column(name = "url")             private String        url;
  @Column(name = "ok")              private boolean       ok;
  @Column(name = "problem")         private String        problem;
  @Column(name = "bucket")          private String        bucket;
  @Column(name = "html_key")        private String        htmlKey;
  @Column(name = "html_length")     private Integer       htmlLength;
  @Column(name = "screenshot_key")  private String        screenshotKey;
  @Column(name = "har_key")         private String        harKey;
  @Column(name = "metrics_json")    private String        metricsJson;
  @Column(name = "crawl_timestamp") private ZonedDateTime crawlTimestamp;
  @Column(name = "ipv4")            private String        ipv4;
  @Column(name = "ipv6")            private String        ipv6;
  @Column(name = "browser_version") private String        browserVersion;
  @Column(name = "final_url")       private String        finalUrl;
  @Column(name = "retries")         private Integer       retries;


  public ContentCrawlResult(UUID visitId, String domainName, String url, boolean ok, String problem, int retries) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.url = url;
    this.ok = ok;
    this.problem = problem;
    this.crawlTimestamp = ZonedDateTime.now();
    this.retries = retries;
  }

  public static ContentCrawlResult of(MuppetsResolution resolution) {
    ContentCrawlResult contentCrawlResult;
    if (resolution.isOk()) {
      contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), true, null, resolution.getRetries());
      contentCrawlResult.bucket = resolution.getBucket();
      contentCrawlResult.htmlKey = resolution.getHtmlFile();
      contentCrawlResult.htmlLength = resolution.getHtmlLength();
      contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
      contentCrawlResult.harKey = resolution.getHarFile();
      contentCrawlResult.metricsJson = resolution.getMetrics();
      contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
    } else {
      contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, resolution.getErrors(), resolution.getRetries());
    }
    contentCrawlResult.ipv4 = resolution.getIpv4();
    contentCrawlResult.ipv6 = resolution.getIpv6();
    contentCrawlResult.browserVersion = resolution.getBrowserVersion();
    return contentCrawlResult;
  }
}
