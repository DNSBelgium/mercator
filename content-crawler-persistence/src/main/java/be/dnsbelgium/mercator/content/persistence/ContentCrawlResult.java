package be.dnsbelgium.mercator.content.persistence;

import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import be.dnsbelgium.mercator.content.dto.Status;
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
  @Column(name = "visit_id")          private UUID          visitId;
  @Column(name = "domain_name")       private String        domainName;
  @Column(name = "url")               private String        url;
  @Column(name = "ok")                private boolean       ok;
  @Column(name = "html_status")       private Status        html_status;
  @Column(name = "screenshot_status") private Status        screenshot_status;
  @Column(name = "problem")           private String        problem;
  @Column(name = "bucket")            private String        bucket;
  @Column(name = "html_key")          private String        htmlKey;
  @Column(name = "html_length")       private Integer       htmlLength;
  @Column(name = "screenshot_key")    private String        screenshotKey;
  @Column(name = "har_key")           private String        harKey;
  @Column(name = "metrics_json")      private String        metricsJson;
  @Column(name = "crawl_timestamp")   private ZonedDateTime crawlTimestamp;
  @Column(name = "ipv4")              private String        ipv4;
  @Column(name = "ipv6")              private String        ipv6;
  @Column(name = "browser_version")   private String        browserVersion;
  @Column(name = "final_url")         private String        finalUrl;
  @Column(name = "retries")           private Integer       retries;


  public ContentCrawlResult(UUID visitId, String domainName, String url, boolean ok, String problem, int retries, Status html_status, Status screenshot_status) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.url = url;
    this.ok = ok;
    this.problem = problem;
    this.crawlTimestamp = ZonedDateTime.now();
    this.retries = retries;
    this.html_status = html_status;
    this.screenshot_status = screenshot_status;
  }

  public static ContentCrawlResult of(MuppetsResolution resolution) {
    ContentCrawlResult contentCrawlResult;
    if (resolution.isOk()) {
      contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), true, null, resolution.getRetries(),Status.Ok,Status.Ok);
      contentCrawlResult.bucket = resolution.getBucket();
      contentCrawlResult.htmlKey = resolution.getHtmlFile();
      contentCrawlResult.htmlLength = resolution.getHtmlLength();
      contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
      contentCrawlResult.harKey = resolution.getHarFile();
      contentCrawlResult.metricsJson = resolution.getMetrics();
      contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
    } else {
      if (resolution.getErrors().contains("Navigation timeout of 15000 ms exceeded")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.TimeOut,Status.TimeOut);
      } else if (resolution.getErrors().contains("net::ERR_NAME_NOT_RESOLVED")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.UploadFailed,Status.UploadFailed);
      } // both html and screenshot not uploaded
      else if (resolution.getErrors().contains("uploading to S3 cancelled, html size bigger then 10Mb:")&resolution.getErrors().contains("screenshot bigger then 10MiB Upload to S3 cancelled")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.HtmlTooBig,Status.screenshotTooBig);
      } // html not uploaded, screenshot did upload
      else if (resolution.getErrors().contains("uploading to S3 cancelled, html size bigger then 10Mb:")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.HtmlTooBig,Status.Ok);
        contentCrawlResult.bucket = resolution.getBucket();
        contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
        contentCrawlResult.harKey = resolution.getHarFile();
        contentCrawlResult.metricsJson = resolution.getMetrics();
        contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
      } // html uploaded screenshot not uploaded
      else if (resolution.getErrors().contains("screenshot bigger then 10MiB Upload to S3 cancelled")) {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.Ok,Status.screenshotTooBig);
        contentCrawlResult.bucket = resolution.getBucket();
        contentCrawlResult.htmlKey = resolution.getHtmlFile();
        contentCrawlResult.htmlLength = resolution.getHtmlLength();
        contentCrawlResult.harKey = resolution.getHarFile();
        contentCrawlResult.metricsJson = resolution.getMetrics();
        contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
      } // both html and screenshot not uploaded
      else if (resolution.getErrors().contains("Upload failed for html file") && resolution.getErrors().contains("Upload failed for screenshot file")){
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.UploadFailed,Status.UploadFailed);
      } // html not uploaded, screenshot did upload
      else if (resolution.getErrors().contains("Upload failed for html file") && !(resolution.getErrors().contains("Upload failed for screenshot file")) ){
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.UploadFailed,Status.Ok);
        contentCrawlResult.bucket = resolution.getBucket();
        contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
        if (!(resolution.getErrors().contains("Upload failed for har file"))){
          contentCrawlResult.harKey = resolution.getHarFile();
        }
        contentCrawlResult.metricsJson = resolution.getMetrics();
        contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
      } // html uploaded screenshot not uploaded
      else if (!(resolution.getErrors().contains("Upload failed for html file")) && resolution.getErrors().contains("Upload failed for screenshot file")){
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, null, resolution.getRetries(),Status.Ok,Status.UploadFailed);
        contentCrawlResult.bucket = resolution.getBucket();
        contentCrawlResult.htmlKey = resolution.getHtmlFile();
        contentCrawlResult.htmlLength = resolution.getHtmlLength();
        if (!(resolution.getErrors().contains("Upload failed for har file"))){
          contentCrawlResult.harKey = resolution.getHarFile();
        }
        contentCrawlResult.metricsJson = resolution.getMetrics();
        contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
      } else {
        contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), false, resolution.getErrors(), resolution.getRetries(),Status.UnexpectedError,Status.UnexpectedError);
      }
    }
    contentCrawlResult.ipv4 = resolution.getIpv4();
    contentCrawlResult.ipv6 = resolution.getIpv6();
    contentCrawlResult.browserVersion = resolution.getBrowserVersion();
    return contentCrawlResult;
  }
}
