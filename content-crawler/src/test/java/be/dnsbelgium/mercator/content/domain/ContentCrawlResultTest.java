package be.dnsbelgium.mercator.content.domain;

import be.dnsbelgium.mercator.content.domain.content.ContentResolutionTest;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentCrawlResultTest {

  //html status tests
  @Test
  void testHtmlStatusTooBig(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestHtmlFailTooBig();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getHtml_status()).isEqualTo("Failed: Html file too big");
    assertThat(contentCrawlResult.getScreenshot_status()).isEqualTo("Success");
    assertThat(contentCrawlResult.getProblem()).isNull();
  }
  @Test
  void testHtmlUploadFail(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestHtmlUploadFail();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getHtml_status()).isEqualTo("Failed: Upload failed");
    assertThat(contentCrawlResult.getProblem()).isNull();

  }
  @Test
  void testHtmlStatusNameNotFound(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestNameNotResolved();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getHtml_status()).isEqualTo("Failed: Name not resolved");
    assertThat(contentCrawlResult.getProblem()).isNull();

  }
  @Test
  void testHtmlTimeOut(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestTimeOut();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getHtml_status()).isEqualTo("Failed: Time out error");
    assertThat(contentCrawlResult.getProblem()).isNull();

  }

  //screenshot status tests
  @Test
  void testScreenhsotStatusTooBig(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestScreenshotFailTooBig();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getScreenshot_status()).isEqualTo("Failed: Screenshot file too big");
    assertThat(contentCrawlResult.getHtml_status()).isEqualTo("Success");
    assertThat(contentCrawlResult.getProblem()).isNull();

  }
  @Test
  void testScreenshotUploadFail(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestScreenshotUploadFail();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getScreenshot_status()).isEqualTo("Failed: Upload failed");
    assertThat(contentCrawlResult.getProblem()).isNull();

  }
  @Test
  void testScreenshotStatusNameNotFound(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestNameNotResolved();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);
    assertThat(contentCrawlResult.getScreenshot_status()).isEqualTo("Failed: Name not resolved");
    assertThat(contentCrawlResult.getProblem()).isNull();

  }
  @Test
  void testScreenshotTimeOut(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestTimeOut();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getScreenshot_status()).isEqualTo("Failed: Time out error");
    assertThat(contentCrawlResult.getProblem()).isNull();
  }

  @Test
  void testUnexpectedError(){
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTestUnexpectedError();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertThat(contentCrawlResult.getHtml_status()).isEqualTo("Failed: Unexpected error");
    assertThat(contentCrawlResult.getScreenshot_status()).isEqualTo("Failed: Unexpected error");
    assertThat(contentCrawlResult.getProblem()).isEqualTo(muppetsResolution.getErrors());
  }

  @Test
  void testOfContentResolution() {
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTest();

    ContentCrawlResult contentCrawlResult = ContentCrawlResult.of(muppetsResolution);

    assertContentResolutionIsEqualToContentCrawlResult(muppetsResolution, contentCrawlResult);
  }

  public static void assertContentResolutionIsEqualToContentCrawlResult(MuppetsResolution muppetsResolution,
                                                                        ContentCrawlResult contentCrawlResult) {
    assertThat(contentCrawlResult.getDomainName()).isEqualTo(muppetsResolution.getDomainName());
    assertThat(contentCrawlResult.getUrl()).isEqualTo(muppetsResolution.getUrl());
    assertThat(contentCrawlResult.getFinalUrl()).isEqualTo(muppetsResolution.getFinalUrl());
    assertThat(contentCrawlResult.getBrowserVersion()).isEqualTo(muppetsResolution.getBrowserVersion());
    assertThat(contentCrawlResult.getBucket()).isEqualTo(muppetsResolution.getBucket());
    assertThat(contentCrawlResult.isOk()).isEqualTo(muppetsResolution.isOk());
    assertThat(contentCrawlResult.getProblem()).isEqualTo(muppetsResolution.getErrors());
    assertThat(contentCrawlResult.getHtmlKey()).isEqualTo(muppetsResolution.getHtmlFile());
    assertThat(contentCrawlResult.getHtmlLength()).isEqualTo(muppetsResolution.getHtmlLength());
    assertThat(contentCrawlResult.getScreenshotKey()).isEqualTo(muppetsResolution.getScreenshotFile());
    assertThat(contentCrawlResult.getHarKey()).isEqualTo(muppetsResolution.getHarFile());
    assertThat(contentCrawlResult.getMetricsJson()).isEqualTo(muppetsResolution.getMetrics());
    assertThat(contentCrawlResult.getRetries()).isEqualTo(muppetsResolution.getRetries());
  }

  // Object Mothers

  public static ContentCrawlResult contentCrawlResult(UUID visitId, String url) {
    ContentCrawlResult contentCrawlResult =
        new ContentCrawlResult(visitId, "dnsbelgium.be", url, true, null, 0, contentCrawlResult().getHtml_status(), contentCrawlResult().getScreenshot_status());
    contentCrawlResult.setBucket("MyBucket");
    contentCrawlResult.setBrowserVersion("Blabla 1.2");
    contentCrawlResult.setHarKey("file.har");
    contentCrawlResult.setHtmlKey("file.html");
    contentCrawlResult.setHtmlLength(10);
    contentCrawlResult.setScreenshotKey("screenshot.png");
    contentCrawlResult.setIpv4("1.2.3.4");
    contentCrawlResult.setIpv6("::0");
    contentCrawlResult.setMetricsJson("{}");
    contentCrawlResult.setRetries(1);
    return contentCrawlResult;
  }

  public static ContentCrawlResult contentCrawlResult(UUID visitId) {
    return contentCrawlResult (visitId, "https://www.dnsbelgium.be");
  }

  public static ContentCrawlResult contentCrawlResult() {
    return contentCrawlResult(UUID.randomUUID());
  }

}
