package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.vat.metrics.MetricName;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class VatScraperTest {

  private VatScraper vatScraper;
  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private static final Logger logger = getLogger(VatScraperTest.class);

  public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @BeforeEach
  public void init() throws IOException {
    //  Creating small test web server to serve web pages to scrape
    stubFor("/", "html/simple/index.html");
    stubFor("/simple/coeck.html", "html/simple/coeck.html");
    for (String s : new String[]{"index", "page-1", "page-2"}) {
      stubFor("/complex/" + s, "html/complex/" + s + ".html");
    }
    for (String s : new String[]{"wrong-checksum", "wrong-format"}) {
      stubFor("/wrong/" + s, "html/wrong/" + s + ".html");
    }
    wireMockRule.start();
    PageFetcher pageFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.defaultConfig());
    LinkPrioritizer linkPrioritizer = new LinkPrioritizer();
    VatFinder vatFinder = new VatFinder();
    vatScraper = new VatScraper(meterRegistry, pageFetcher, vatFinder, linkPrioritizer);
  }

  @Test
  void vatFoundOnFirstPage() {
    // Easy case: VAT on first page
    Counter pagesFetched = meterRegistry.counter(MetricName.COUNTER_PAGES_FETCHED);
    Counter sitesWithVat = meterRegistry.counter(MetricName.COUNTER_SITES_WITH_VAT);
    double pagesFetchedBefore = pagesFetched.count();
    double sitesWithVatBefore = sitesWithVat.count();
    SiteVisit visit = vatScraper.visit(urlFor("/"), 1);
    List<String> vatValues = visit.getVatValues();
    logger.info("vat = {}", vatValues);
    assertThat(vatValues).containsExactly("BE0542703815");
    assertThat(visit.getNumberOfVisitedPages()).isEqualTo(1);
    assertThat(pagesFetched.count()).isEqualTo(pagesFetchedBefore + 1);
    assertThat(sitesWithVat.count()).isEqualTo(sitesWithVatBefore + 1);
  }

  @Test
  void noVatOnFirstPage() {
    Counter sitesWithVat = meterRegistry.counter(MetricName.COUNTER_SITES_WITH_VAT);
    Counter sitesWithoutVat = meterRegistry.counter(MetricName.COUNTER_SITES_WITHOUT_VAT);
    double sitesWithVatBefore = sitesWithVat.count();
    double sitesWithoutVatBefore = sitesWithoutVat.count();
    // VAT not on first page and depth too small to visit other pages
    SiteVisit visit = vatScraper.visit(urlFor("/complex/index"), 1);
    List<String> vatValues = visit.getVatValues();
    logger.info("vat = {}", vatValues);
    assertThat(vatValues).isEmpty();
    assertThat(visit.getNumberOfVisitedPages()).isEqualTo(1);
    assertThat(sitesWithoutVat.count()).isEqualTo(sitesWithoutVatBefore + 1);
    assertThat(sitesWithVat.count()).isEqualTo(sitesWithVatBefore);
  }

  @Test
  void vatFoundOnSecondPage() {
    SiteVisit visit = vatScraper.visit(urlFor("/complex/index"), 3);
    List<String> vatValues = visit.getVatValues();
    logger.info("vat = {}", vatValues);
    // VAT not on first page and depth large enough to find it on second page
    assertThat(vatValues).containsExactly("BE0542703815");
    assertThat(visit.getNumberOfVisitedPages()).isGreaterThan(1);
  }

  @Test
  public void wrongFormat() {
    // VAT not found because of wrong format
    SiteVisit visit = vatScraper.visit(urlFor("/wrong/wrong-format"), 2);
    List<String> vatValues = visit.getVatValues();
    logger.info("vat = {}", vatValues);
    // VAT not on first page and depth large enough to find it on second page
    assertThat(vatValues).isEmpty();
    assertThat(visit.getNumberOfVisitedPages()).isEqualTo(1);
  }

  @Test
  public void wrongCheckSum() {
    SiteVisit visit = vatScraper.visit(urlFor("/wrong/wrong-checksum"), 2);
    List<String> vatValues = visit.getVatValues();
    logger.info("vat = {}", vatValues);
    logger.info("wrong checksum => vat = {}", vatValues);
    assertThat(vatValues).isEmpty();
    assertThat(visit.getNumberOfVisitedPages()).isEqualTo(1);
  }

  @SneakyThrows
  private HttpUrl urlFor(String path)  {
    return HttpUrl.get("http://127.0.0.1:" + wireMockRule.port() + path);
  }

  private void stubFor(String url, String resourcePath) throws IOException {
    byte[] body = FileCopyUtils.copyToByteArray(new ClassPathResource(resourcePath).getInputStream());
    logger.debug("Setting up stub content for url={} => {}", url, resourcePath);
    wireMockRule.stubFor(get(urlEqualTo(url))
        .willReturn(aResponse()
            .withBody(body)
            .withHeader("Content-Type", "text/html; charset=UTF-8"))
    );
  }

  @Test
  @Disabled
  public void fetchPageAndParseTestWith1PageThatErrors() throws IOException {
    HttpUrl baseUrl;
    String BIG_BODY = StringUtils.repeat("abcdefghjiklmnopqrst", 10_000_000);
    try (MockWebServer mockWebServer = new MockWebServer()) {
      MockResponse response = new MockResponse()
        .setChunkedBody(BIG_BODY, 100);
      PageFetcher testFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.testConfig());
      testFetcher.clearCache();
      VatScraper testVatScraper = new VatScraper(meterRegistry, testFetcher, new VatFinder(), new LinkPrioritizer());
      mockWebServer.enqueue(response);
      mockWebServer.enqueue(new MockResponse().setBody("test"));
      mockWebServer.start();
      baseUrl = mockWebServer.url("/");
      Page page1 = testVatScraper.fetchAndParse(baseUrl);
      assertThat(page1).isEqualTo(null);
      testFetcher.clearCache();
      Page page2 = testVatScraper.fetchAndParse(baseUrl);
      logger.info("page = {}", page2);
      assertThat(page2.getStatusCode()).isEqualTo(200);
      assertThat(page2.getResponseBody()).isEqualTo("test");
    }
  }

}