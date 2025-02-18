package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
@Disabled("since this test depends on internet access and on the state of the sites it tries to visit")
public class VatScraperIntegrationTest {

   /*
    This test depends on internet access since it tries to access real websites
    It's main purpose is debugging edge cases.
    Ideally the edge cases become proper tests that can run locally
   */

  private VatScraper vatScraper;
  private static final Logger logger = getLogger(VatScraperIntegrationTest.class);
  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @BeforeEach
  public void init() {
    PageFetcher pageFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.defaultConfig());
    LinkPrioritizer linkPrioritizer = new LinkPrioritizer();
    linkPrioritizer.init();
    VatFinder vatFinder = new VatFinder();
    vatScraper = new VatScraper(meterRegistry, pageFetcher, vatFinder, linkPrioritizer);
  }

  private SiteVisit scrapeForReal(String link) {
    HttpUrl url = HttpUrl.get(link);
    SiteVisit visit = vatScraper.visit(url, 10);
    for (Page page : visit.getVisitedPages().values()) {
      logger.info("we visited {}", page.getUrl());
    }
    logger.info("visit = {}", visit);
    return visit;
  }

  @Test
  @DisplayName("http://www.dnsbelgium.be")
  public void dnsbelgium() {
    SiteVisit siteVisit = scrapeForReal("http://www.dnsbelgium.be/");
    assertThat(siteVisit.getVatValues()).contains("BE0466158640");
  }

  @Test
  public void fluxio() {
    SiteVisit siteVisit = scrapeForReal("https://www.fluxxio.be/");
    logger.info("siteVisit.getVatValues() = {}", siteVisit.getVatValues());
    assertThat(siteVisit.getVatValues()).contains("BE0727432692");
  }

  @Test
  public void testNPE() {
    // this URL used to generate a NullPointerException, but issue is fixed
    scrapeForReal("http://coeck-centraleverwarming.be/");
  }

  @Test
  public void testRedirect() {
    scrapeForReal("http://www.bartscher.be/");
  }

  @Test
  @DisplayName("http://www.memita.be")
  public void memita_be() {
    // this website generated an ERROR: invalid byte sequence for encoding "UTF8": 0x00"
    SiteVisit siteVisit = scrapeForReal("http://www.memita.be/");
    VisitRequest request = new VisitRequest("memita.be");
    for (Page page : siteVisit.getVisitedPages().values()) {
      logger.info("we visited {} => {}", page.getUrl(), page);
      String body = page.getDocument().body().text();
      if (body.contains("\u0000")) {
        logger.info("body of {} contains 0x00", page.getUrl());
      }
      PageVisit pageVisit = page.asPageVisit(request, false);
      logger.info("pageVisit = {}", pageVisit);
    }
  }

  @Test
  public void memoryUsage() {
    // VAT crawler used to consume a lot of memory when visiting some sites
    // We have improved it by properly avoiding the processing of big files
    memoryStats();
    String domainName = "wawawawa.be";
    SiteVisit siteVisit = scrapeForReal("http://www." + domainName);
    memoryStats();
    logger.info("siteVisit.getMatchingURL      = {}", siteVisit.getMatchingURL());
    logger.info("siteVisit.getMatchingLinkText = {}", siteVisit.getMatchingLinkText());
    VisitRequest request = new VisitRequest(domainName);
    Set<Link> links = siteVisit.getVisitedPages().keySet();
    for (Link link : links) {
      Page page = siteVisit.getVisitedPages().get(link);
      PageVisit pageVisit = page.asPageVisit(request, true);
      logger.info("pageVisit url.length= {}", pageVisit.getUrl().length());
    }
    memoryStats();
    System.gc();
    System.gc();
    memoryStats();
  }

  public static void memoryStats() {
    int mb = 1024 * 1024;
    // get Runtime instance
    Runtime instance = Runtime.getRuntime();
    System.out.println("***** Heap utilization statistics [MB] *****");
    System.out.println("Total Memory: " + instance.totalMemory() / mb);
    System.out.println("Free Memory: " + instance.freeMemory() / mb);
    System.out.println("Used Memory: "
        + (instance.totalMemory() - instance.freeMemory()) / mb);
    System.out.println("Max Memory: " + instance.maxMemory() / mb);
  }
}
