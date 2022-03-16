package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisit;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisitRepository;
import be.dnsbelgium.mercator.vat.crawler.persistence.VatCrawlResult;
import be.dnsbelgium.mercator.vat.crawler.persistence.VatCrawlResultRepository;
import be.dnsbelgium.mercator.vat.domain.*;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
@SpringBootTest (
    properties = {
        "vat.crawler.max.visits.per.domain=7",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, " +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration, " +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
    })
class VatCrawlerServiceTest {

  @Autowired VatCrawlerService vatCrawlerService;
  @MockBean  PageVisitRepository pageVisitRepository;
  @MockBean  VatCrawlResultRepository vatCrawlResultRepository;
  @MockBean  AckMessageService ackMessageService;
  @MockBean  PageFetcher pageFetcher;
  @MockBean  VatScraper vatScraper;

  private static final Logger logger = getLogger(VatCrawlerServiceTest.class);

  @BeforeEach
  public void init() {
    // reset to defaults
    vatCrawlerService.setPersistBodyText(false);
    vatCrawlerService.setPersistPageVisits(false);
    vatCrawlerService.setMaxVisitsPerDomain(7);
  }

  @Test
  public void visitOnePage() {
    Instant started  = Instant.now();
    UUID visitId = UUID.randomUUID();
    HttpUrl url = HttpUrl.get("http://www.dnsbelgium.be");
    Page page = makePage(url, "<htm>body text is not saved</html>");
    SiteVisit siteVisit = new SiteVisit(url);
    siteVisit.add(new Link(url, "test"), page);
    when(vatScraper.visit(url, 7)).thenReturn(siteVisit);

    vatCrawlerService.setPersistFirstPageVisit(false);

    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");
    vatCrawlerService.findVatValues(visitRequest);

    ArgumentCaptor<VatCrawlResult> argumentCaptor = ArgumentCaptor.forClass(VatCrawlResult.class);
    verify(vatCrawlResultRepository).save(argumentCaptor.capture());

    VatCrawlResult vatCrawlResult = argumentCaptor.getValue();
    logger.info("vatCrawlResult = {}", vatCrawlResult);
    assertThat(vatCrawlResult.getVisitId()).isEqualTo(visitId);
    assertThat(vatCrawlResult.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(vatCrawlResult.getStartUrl()).isEqualTo(url.toString());
    assertThat(vatCrawlResult.getCrawlStarted()).isAfterOrEqualTo(started);
    assertThat(vatCrawlResult.getCrawlFinished()).isAfterOrEqualTo(started);
    assertThat(vatCrawlResult.getVisitedUrls()).contains(url.toString());

    verify(pageVisitRepository, never()).save(any(PageVisit.class));
  }

  @Test
  public void testSaveLandingPage() {
    UUID visitId = UUID.randomUUID();
    HttpUrl url1 = HttpUrl.get("http://www.dnsbelgium.be");
    HttpUrl url2 = HttpUrl.get("http://www.dnsbelgium.be/contact");

    Page page1 = makePage(url1, "<html>hi there</html>");
    Page page2 = makePage(url2, "<html>this is page 2</html>");
    SiteVisit siteVisit = new SiteVisit(url1);
    siteVisit.add(new Link(url1, "test1"), page1);
    siteVisit.add(new Link(url2, "test2"), page2);
    when(vatScraper.visit(url1, 7)).thenReturn(siteVisit);

    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");

    vatCrawlerService.setPersistPageVisits(false);
    vatCrawlerService.setPersistFirstPageVisit(true);
    vatCrawlerService.findVatValues(visitRequest);

    ArgumentCaptor<PageVisit> pageCaptor = ArgumentCaptor.forClass(PageVisit.class);
    verify(pageVisitRepository, times(1)).save(pageCaptor.capture());

    List<PageVisit> pageVisits = pageCaptor.getAllValues();
    logger.info("pageVisits.size = {}", pageVisits.size());
    assertThat(pageVisits.size()).isEqualTo(1);

    PageVisit pageVisit1 = pageVisits.get(0);
    logger.info("pageVisit1 = {}", pageVisit1);
    assertThat(pageVisit1.getBodyText()).isEqualTo("hi there");
  }

  @Test
  public void savePageVisits() {
    UUID visitId = UUID.randomUUID();
    HttpUrl url1 = HttpUrl.get("http://www.dnsbelgium.be");
    HttpUrl url2 = HttpUrl.get("http://www.dnsbelgium.be/contact");

    Page page1 = makePage(url1, "<html>hi there</html>");
    Page page2 = makePage(url2, "<html>this is page 2</html>");
    SiteVisit siteVisit = new SiteVisit(url1);
    siteVisit.add(new Link(url1, "test1"), page1);
    siteVisit.add(new Link(url2, "test2"), page2);
    when(vatScraper.visit(url1, 7)).thenReturn(siteVisit);

    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");

    vatCrawlerService.setPersistPageVisits(true);
    vatCrawlerService.setPersistFirstPageVisit(false);
    vatCrawlerService.findVatValues(visitRequest);

    ArgumentCaptor<PageVisit> pageCaptor = ArgumentCaptor.forClass(PageVisit.class);
    verify(pageVisitRepository, times(2)).save(pageCaptor.capture());

    List<PageVisit> pageVisits = pageCaptor.getAllValues();
    logger.info("pageVisits.size = {}", pageVisits.size());
    assertThat(pageVisits.size()).isEqualTo(2);

    PageVisit pageVisit1 = pageVisits.get(0);
    PageVisit pageVisit2 = pageVisits.get(1);
    logger.info("pageVisit1 = {}", pageVisit1);
    logger.info("pageVisit2 = {}", pageVisit2);


    assertThat(pageVisit1.getVisitId()).isEqualTo(visitId);
    assertThat(pageVisit2.getVisitId()).isEqualTo(visitId);
    assertThat(pageVisit1.getUrl()).isEqualTo(url1.toString());
    assertThat(pageVisit2.getUrl()).isEqualTo(url2.toString());
    assertThat(pageVisit1.getBodyText()).isNull();
    assertThat(pageVisit1.getCrawlStarted()).isEqualTo(page1.getVisitStarted());
    assertThat(pageVisit1.getCrawlFinished()).isEqualTo(page1.getVisitFinished());
  }

  @Test
  public void saveBodyText() {
    UUID visitId = UUID.randomUUID();
    HttpUrl url = HttpUrl.get("http://www.dnsbelgium.be");
    Page page = makePage(url, "<h1>hi there</h1>");
    SiteVisit siteVisit = new SiteVisit(url);
    siteVisit.add(new Link(url, "test"), page);
    when(vatScraper.visit(url, 7)).thenReturn(siteVisit);

    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");
    vatCrawlerService.setPersistPageVisits(true);
    vatCrawlerService.setPersistBodyText(true);
    vatCrawlerService.findVatValues(visitRequest);

    ArgumentCaptor<PageVisit> pageCaptor = ArgumentCaptor.forClass(PageVisit.class);
    verify(pageVisitRepository).save(pageCaptor.capture());
    PageVisit pageVisit = pageCaptor.getValue();

    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getVisitId()).isEqualTo(visitId);
    assertThat(pageVisit.getUrl()).isEqualTo(url.toString());
    assertThat(pageVisit.getBodyText()).isEqualTo("hi there");
  }

  @Test
  public void alwaysSavePageWithVat() {
    UUID visitId = UUID.randomUUID();
    HttpUrl url1 = HttpUrl.get("http://www.dnsbelgium.be");
    HttpUrl url2 = HttpUrl.get("http://www.dnsbelgium.be/contact");

    Page page1 = makePage(url1, "<html>hi there</html>");
    //Page page2 = makePage(url2, "<html>this is another page without a VAT number</html>");
    Page page2 = makePage(url2, "<html>this is page 2 with a VAT number</html>");
    page2.setVatValues(List.of("0466158640"));
    SiteVisit siteVisit = new SiteVisit(url1);
    siteVisit.add(new Link(url1, "test1"), page1);
    siteVisit.add(new Link(url2, "test2"), page2);
    //siteVisit.add(new Link(url3, "test2"), page3);
    when(vatScraper.visit(url1, 7)).thenReturn(siteVisit);

    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");

    vatCrawlerService.setPersistPageVisits(false);
    vatCrawlerService.setPersistBodyText(false);
    vatCrawlerService.setPersistFirstPageVisit(false);
    vatCrawlerService.findVatValues(visitRequest);

    ArgumentCaptor<PageVisit> pageCaptor = ArgumentCaptor.forClass(PageVisit.class);
    verify(pageVisitRepository, times(1)).save(pageCaptor.capture());

    List<PageVisit> pageVisits = pageCaptor.getAllValues();
    logger.info("pageVisits.size = {}", pageVisits.size());
    assertThat(pageVisits.size()).isEqualTo(1);

    PageVisit pageVisit1 = pageVisits.get(0);
    logger.info("pageVisit1 = {}", pageVisit1);

    assertThat(pageVisit1.getVisitId()).isEqualTo(visitId);
    assertThat(pageVisit1.getUrl()).isEqualTo(url2.toString());
    assertThat(pageVisit1.getBodyText()).isEqualTo("this is page 2 with a VAT number");
  }

  private Page makePage(HttpUrl url, String response) {
    Instant pageRequested = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant pageRetrieved = pageRequested.plusMillis(100);
    return new Page(url, pageRequested, pageRetrieved, 200, response, 100, MediaType.parse("text/html"));
  }

}