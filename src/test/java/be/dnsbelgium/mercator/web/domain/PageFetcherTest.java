package be.dnsbelgium.mercator.web.domain;


import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.web.metrics.MetricName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
class PageFetcherTest {

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final PageFetcher pageFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.defaultConfig());
  private static final Logger logger = getLogger(PageFetcherTest.class);

  private static boolean isHttpbinDisabled() {
    return true;
  }

  private static boolean avoidInternet() {
    return true;
  }



  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  @Test
  public void fetchGoogle() throws IOException {
    HttpUrl url = HttpUrl.get("http://www.google.be");
    Instant beforeFetch = TestUtils.now();
    pageFetcher.clearCache();
    Page page = pageFetcher.fetch(url);
    logger.info("page = {}", page);
    assertThat(page.getUrl().toString()).contains("www.google.be");
    assertThat(page.getResponseBody().length()).isGreaterThan(1);
    assertThat(page.getVisitStarted()).isAfterOrEqualTo(beforeFetch);
    assertThat(page.getVisitFinished()).isAfterOrEqualTo(beforeFetch);
    assertThat(page.getStatusCode()).isEqualTo(200);

    Page fetchAgain = pageFetcher.fetch(url);
    assertThat(fetchAgain.getVisitStarted()).isEqualTo(page.getVisitStarted());

  }

  @Test
  @DisabledIf(value = "isHttpbinDisabled", disabledReason="Tests using HttpBin are disabled (sometimes it is very slow")
  public void visitDuration() throws IOException {
    Page page = pageFetcher.fetch(HttpUrl.get("http://httpbin.org/delay/0.5"));
    Instant started = page.getVisitStarted();
    Instant finished = page.getVisitFinished();
    Duration duration = Duration.between(started, finished);
    logger.info("page.getVisitDuration = {}", duration);
    assertThat(duration.toMillis()).isBetween(500L, 2000L);
  }

  @Test
  public void fetchFromUnknownHost() {
    assertThrows(UnknownHostException.class,
        () -> pageFetcher.fetch(HttpUrl.get("http://no.webserver.at.this.url")));
  }

//  @Test
//  public void test404() throws IOException {
//    var path = "/this-page-returns-404";
//    var body = "hello, I am a 404";
//    client
//            .when(request().withPath(path))
//            .respond(response()
//                            .withStatusCode(404)
//                            .withBody(body)
//            );
//    Page page = pageFetcher.fetch(url(path));
//    logger.info("page = {}", page);
//    logger.info("page.statusCode = {}", page.getStatusCode());
//    assertThat(page.getStatusCode()).isEqualTo(404);
//    assertThat(page.getDocument().text()).isEqualTo(body);
//  }
//
//  @Test
//  public void close() throws IOException {
//    client.when(request().withPath("/")).respond(response().withStatusCode(200));
//    Page page = pageFetcher.fetch(url("/"));
//    assertThat(page.getStatusCode()).isEqualTo(200);
//    pageFetcher.close();
//    // a fetch call will now throw an IllegalStateException
//    assertThrows(IllegalStateException.class,
//        () -> pageFetcher.fetch(url("/")));
//  }

  @Test
  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  public void fetchPdf() throws IOException {
    String url = "https://assets.dnsbelgium.be/attachment/Wijziging-gemachtigde-DomainGuard-nl_0.pdf";
    Page page = pageFetcher.fetch(HttpUrl.get(url));
    logger.info("page = {}", page);
    assertThat(page.getStatusCode()).isEqualTo(Page.SpecialPageStatus.CONTENT_TYPE_NOT_SUPPORTED.getCode());
  }

  @Test
  @DisabledIf(value = "isHttpbinDisabled", disabledReason="Tests using HttpBin are disabled (sometimes it is very slow")
  public void fetchPng() throws IOException {
    HttpUrl url = HttpUrl.get("http://httpbin.org/image/png");
    Page page = pageFetcher.fetch(url);
    logger.info("page = {}", page);
    assertThat(page.getStatusCode()).isEqualTo(Page.SpecialPageStatus.CONTENT_TYPE_NOT_SUPPORTED.getCode());
  }

  @Test
  @DisabledIf(value = "isHttpbinDisabled", disabledReason="Tests using HttpBin are disabled (sometimes it is very slow")
  public void fetchSvg() throws IOException {
    HttpUrl url = HttpUrl.get("http://httpbin.org/image/svg");
    Page page = pageFetcher.fetch(url);
    logger.info("page = {}", page);
    assertThat(page.getStatusCode()).isEqualTo(Page.SpecialPageStatus.CONTENT_TYPE_NOT_SUPPORTED.getCode());
  }

  @Test
  @DisabledIf(value = "isHttpbinDisabled", disabledReason="Tests using HttpBin are disabled (sometimes it is very slow")
  public void fetchWebp() throws IOException {
    HttpUrl url = HttpUrl.get("http://httpbin.org/image/webp");
    Page page = pageFetcher.fetch(url);
    logger.info("page = {}", page);
    assertThat(page.getStatusCode()).isEqualTo(Page.SpecialPageStatus.CONTENT_TYPE_NOT_SUPPORTED.getCode());
  }

  @Test
  @DisabledIf(value = "isHttpbinDisabled", disabledReason="Tests using HttpBin are disabled (sometimes it is very slow")
  public void fetchJpeg() throws IOException {
    HttpUrl url = HttpUrl.get("http://httpbin.org/image/jpeg");
    Counter counter = meterRegistry.counter(MetricName.COUNTER_PAGES_CONTENT_TYPE_NOT_SUPPORTED,
        "content-type", "image/jpeg");
    double before = counter.count();
    Page page = pageFetcher.fetch(url);
    logger.info("page = {}", page);
    assertThat(page.getStatusCode()).isEqualTo(Page.SpecialPageStatus.CONTENT_TYPE_NOT_SUPPORTED.getCode());
    assertThat(counter.count()).isEqualTo(before + 1);
  }

  @Test
  @DisabledIf(value = "isHttpbinDisabled", disabledReason="Tests using HttpBin are disabled (sometimes it is very slow")
  public void headers() throws IOException {
    HttpUrl url = HttpUrl.get("http://httpbin.org/anything");
    Page page = pageFetcher.fetch(url);
    logger.info("page = {}", page);
    logger.info("page.text = {}", page.getDocument().text());
    assertThat(page.getDocument().text()).contains("\"User-Agent\": \"Mozilla/5.0 ");
  }


  @Test
  @DisplayName("http://www.dnsbelgium.com/")
  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  public void http_dnsbelgium_dot_com() throws IOException {
    Page page = pageFetcher.fetch(HttpUrl.get("http://www.dnsbelgium.com/"));
    logger.info("page = {}", page);
    assertThat(page.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void fetchPageWithoutContentLengthHeaderAndBodyLengthOverMax() throws IOException {
    HttpUrl baseUrl;
    String BIG_BODY = StringUtils.repeat("ab", 100_000);
    try (MockWebServer mockWebServer = new MockWebServer()) {
      MockResponse response = new MockResponse()
        .setChunkedBody(BIG_BODY, 100);
      PageFetcher testFetcher = new PageFetcher(meterRegistry, TestPageFetcherConfig.testConfig());
      testFetcher.clearCache();
      mockWebServer.enqueue(response);
      mockWebServer.start();
      baseUrl = mockWebServer.url("/");
      testFetcher.clearCache();
      Page page = testFetcher.fetch(baseUrl);
      logger.info("page = {}", page);
      assertEquals(TestPageFetcherConfig.testConfig().getMaxContentLength().toBytes(), page.getResponseBody().length());
    }
  }

}