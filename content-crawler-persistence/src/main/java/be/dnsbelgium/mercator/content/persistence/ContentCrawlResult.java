package be.dnsbelgium.mercator.content.persistence;

import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import be.dnsbelgium.mercator.content.dto.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.net.IDN;
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
  @Column(name = "visit_id")
  private UUID visitId;
  @Column(name = "domain_name")
  private String domainName;
  @Column(name = "url")
  private String url;
  @Column(name = "ok")
  private boolean ok;

  @Column(name = "html_status")
  @Convert(converter = StatusJpaConverter.class)
  private Status htmlStatus;

  @Column(name = "screenshot_status")
  @Convert(converter = StatusJpaConverter.class)
  private Status screenshotStatus;

  @Column(name = "problem")
  private String problem;
  @Column(name = "bucket")
  private String bucket;
  @Column(name = "html_key")
  private String htmlKey;
  @Column(name = "html_length")
  private Integer htmlLength;
  @Column(name = "screenshot_key")
  private String screenshotKey;
  @Column(name = "har_key")
  private String harKey;
  @Column(name = "metrics_json")
  private String metricsJson;
  @Column(name = "crawl_timestamp")
  private ZonedDateTime crawlTimestamp;
  @Column(name = "ipv4")
  private String ipv4;
  @Column(name = "ipv6")
  private String ipv6;
  @Column(name = "browser_version")
  private String browserVersion;
  @Column(name = "final_url")
  private String finalUrl;
  @Column(name = "retries")
  private Integer retries;


  public ContentCrawlResult(UUID visitId, String domainName, String url, boolean ok, String problem, int retries, Status htmlStatus, Status screenshotStatus) {
    this.visitId = visitId;
    this.domainName = IDN.toUnicode(domainName);
    this.url = url;
    this.ok = ok;
    this.problem = problem;
    this.crawlTimestamp = ZonedDateTime.now();
    this.retries = retries;
    this.htmlStatus = htmlStatus;
    this.screenshotStatus = screenshotStatus;
  }

  public static ContentCrawlResult of(MuppetsResolution resolution) {
    ContentCrawlResult contentCrawlResult = new ContentCrawlResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(), true, null, resolution.getRetries(), Status.Ok, Status.Ok);
    contentCrawlResult.ipv4 = resolution.getIpv4();
    contentCrawlResult.ipv6 = resolution.getIpv6();
    contentCrawlResult.browserVersion = resolution.getBrowserVersion();

    if (!resolution.isOk()) {
      return convertErrorsToResult(resolution, contentCrawlResult);
    }

    contentCrawlResult.bucket = resolution.getBucket();
    contentCrawlResult.htmlKey = resolution.getHtmlFile();
    contentCrawlResult.htmlLength = resolution.getHtmlLength();
    contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
    contentCrawlResult.metricsJson = resolution.getMetrics();
    contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);

    if (resolution.isHtmlSkipped()) {
      contentCrawlResult.htmlKey = null;
      contentCrawlResult.htmlLength = null;
      contentCrawlResult.htmlStatus = Status.HtmlTooBig;
    }

    if (resolution.isScreenshotSkipped()) {
      contentCrawlResult.screenshotKey = null;
      contentCrawlResult.screenshotStatus = Status.ScreenshotTooBig;
    }

    if (!resolution.isHarSkipped()) {
      contentCrawlResult.harKey = resolution.getHarFile();
    }

    return contentCrawlResult;
  }

  private static ContentCrawlResult convertErrorsToResult(MuppetsResolution resolution, ContentCrawlResult contentCrawlResult) {
    contentCrawlResult.ok = false;
    if (resolution.getErrors().contains("Navigation timeout")) {
      contentCrawlResult.screenshotStatus = Status.TimeOut;
      contentCrawlResult.htmlStatus = Status.TimeOut;
    } else if (resolution.getErrors().contains("net::ERR_NAME_NOT_RESOLVED")) {
      contentCrawlResult.screenshotStatus = Status.NameNotResolved;
      contentCrawlResult.htmlStatus = Status.NameNotResolved;
    } else if (resolution.getErrors().contains("Upload failed for html file") && resolution.getErrors().contains("Upload failed for screenshot file")) {
      // both html and screenshot not uploaded
      contentCrawlResult.screenshotStatus = Status.UploadFailed;
      contentCrawlResult.htmlStatus = Status.UploadFailed;
    } else if (resolution.getErrors().contains("Upload failed for html file") && !(resolution.getErrors().contains("Upload failed for screenshot file"))) {
      // html not uploaded, screenshot did upload
      contentCrawlResult.htmlStatus = Status.UploadFailed;
      contentCrawlResult.bucket = resolution.getBucket();
      contentCrawlResult.screenshotKey = resolution.getScreenshotFile();
      checkHar(resolution, contentCrawlResult);
      contentCrawlResult.metricsJson = resolution.getMetrics();
      contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
    } else if (!(resolution.getErrors().contains("Upload failed for html file")) && resolution.getErrors().contains("Upload failed for screenshot file")) {
      // html uploaded screenshot not uploaded
      contentCrawlResult.screenshotStatus = Status.UploadFailed;
      contentCrawlResult.bucket = resolution.getBucket();
      contentCrawlResult.htmlKey = resolution.getHtmlFile();
      contentCrawlResult.htmlLength = resolution.getHtmlLength();
      checkHar(resolution, contentCrawlResult);
      contentCrawlResult.metricsJson = resolution.getMetrics();
      contentCrawlResult.finalUrl = StringUtils.abbreviate(resolution.getFinalUrl(), 2100);
    } else {
      //unexpected error
      contentCrawlResult.problem = resolution.getErrors();
      contentCrawlResult.screenshotStatus = Status.UnexpectedError;
      contentCrawlResult.htmlStatus = Status.UnexpectedError;
    }
    return contentCrawlResult;
  }

  private static void checkHar(MuppetsResolution resolution, ContentCrawlResult contentCrawlResult) {
    if (!(resolution.getErrors().contains("Upload failed for har file"))) {
      contentCrawlResult.harKey = resolution.getHarFile();
    }
  }
}
