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
  @Column(name = "crawl_status")    private String        crawl_status;
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


  public ContentCrawlResult(UUID visitId, String domainName, String url, String crawl_succesfull, String problem, int retries) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.url = url;
    this.crawl_status = crawl_succesfull;
    this.problem = problem;
    this.crawlTimestamp = ZonedDateTime.now();
    this.retries = retries;
  }

  public enum ErrorDescriptions
  {
    E0("Succes"),
    E1("Failed: Time out error"),
    E2("Failed: Html file to big"),
    E3("Failed: Screenshot file to big"),
    E4("Failed: Upload failed"),
    E5("Failed: Unexpected error");

    private final String errorDescription;

    ErrorDescriptions(String errorDescription) {
      this.errorDescription = errorDescription;
    }

    public String getErrorDescription() {
      return errorDescription;
    }
  }

  public static ContentCrawlResult of(MuppetsResolution resolution) {
    ErrorDescriptions error0 =ErrorDescriptions.E0;
    ErrorDescriptions error1 =ErrorDescriptions.E1;
    ErrorDescriptions error2 =ErrorDescriptions.E2;
    ErrorDescriptions error3 =ErrorDescriptions.E3;
    ErrorDescriptions error4 =ErrorDescriptions.E4;
    ErrorDescriptions error5 =ErrorDescriptions.E5;

    ContentCrawlResult contentCrawlResult;
    if (resolution.isOk()) {
      contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), error0.getErrorDescription(), null, resolution.getRetries());
      contentCrawlResult.bucket = resolution.getBucket();
      contentCrawlResult.htmlKey = resolution.getHtmlFile();
      contentCrawlResult.htmlLength = resolution.getHtmlLength();
      contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
      contentCrawlResult.harKey = resolution.getHarFile();
      contentCrawlResult.metricsJson = resolution.getMetrics();
      contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
    } else {
      if (resolution.getErrors().contains("Navigation timeout of 15000 ms exceeded")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), error1.getErrorDescription(), resolution.getErrors(), resolution.getRetries());
      } else if (resolution.getErrors().contains("uploading to S3 cancelled, html size bigger then 10Mb:")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), error2.getErrorDescription(), resolution.getErrors(), resolution.getRetries());
      } else if (resolution.getErrors().contains("screenshot bigger then 10MiB Upload to S3 cancelled")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), error3.getErrorDescription(), resolution.getErrors(), resolution.getRetries());
      } else if (resolution.getErrors().contains("Upload failed for file")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), error4.getErrorDescription(), resolution.getErrors(), resolution.getRetries());
      } else {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), error5.getErrorDescription(), resolution.getErrors(), resolution.getRetries());
      }
    }
    contentCrawlResult.ipv4 = resolution.getIpv4();
    contentCrawlResult.ipv6 = resolution.getIpv6();
    contentCrawlResult.browserVersion = resolution.getBrowserVersion();
    return contentCrawlResult;
  }
}
