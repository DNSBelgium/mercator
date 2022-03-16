package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisit;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisitRepository;
import be.dnsbelgium.mercator.vat.crawler.persistence.VatCrawlResult;
import be.dnsbelgium.mercator.vat.crawler.persistence.VatCrawlResultRepository;
import be.dnsbelgium.mercator.vat.domain.Link;
import be.dnsbelgium.mercator.vat.domain.Page;
import be.dnsbelgium.mercator.vat.domain.SiteVisit;
import be.dnsbelgium.mercator.vat.domain.VatScraper;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VatCrawlerService {

  private static final Logger logger = getLogger(VatCrawlerService.class);

  private final VatScraper vatScraper;
  private final VatCrawlResultRepository vatCrawlResultRepository;
  private final PageVisitRepository pageVisitRepository;

  @Value("${vat.crawler.max.visits.per.domain:10}")
  private int maxVisitsPerDomain = 10;

  @Value("${vat.crawler.persist.page.visits:false}")
  private boolean persistPageVisits = false;

  @Value("${vat.crawler.persist.first.page.visit:false}")
  private boolean persistFirstPageVisit = false;

  // during debugging it could be useful to save the content of each visited page
  @Value("${vat.crawler.persist.body.text:false}")
  private boolean persistBodyText = false;

  @Autowired
  public VatCrawlerService(VatScraper vatScraper, VatCrawlResultRepository vatCrawlResultRepository, PageVisitRepository pageVisitRepository) {
    this.vatScraper = vatScraper;
    this.vatCrawlResultRepository = vatCrawlResultRepository;
    this.pageVisitRepository = pageVisitRepository;
  }

  @PostConstruct
  public void init() {
    logger.info("maxVisitsPerDomain={}", maxVisitsPerDomain);
    logger.info("persistPageVisits={}", persistPageVisits);
    logger.info("persistBodyText={}", persistBodyText);
    logger.info("persistFirstPageVisit={}", persistFirstPageVisit);
  }

  public void findVatValues(VisitRequest visitRequest) {
    String fqdn = visitRequest.getDomainName();
    logger.debug("Searching VAT info for domainName={} and visitId={}", fqdn, visitRequest.getVisitId());

    @SuppressWarnings("HttpUrlsUsage")
    String startURL = "http://www." + fqdn;

    HttpUrl url = HttpUrl.parse(startURL);

    if (url == null) {
      // this is probably a bug: log + throw
      logger.error("VisitRequest {} => invalid URL: {} ", visitRequest, startURL);
      String message = String.format("visitRequest %s lead to invalid url: %s", visitRequest, url);
      throw new RuntimeException(message);
    }

    Instant started = Instant.now();
    SiteVisit siteVisit = vatScraper.visit(url, maxVisitsPerDomain);
    Instant finished = Instant.now();

    logger.debug("siteVisit = {}", siteVisit);
    VatCrawlResult crawlResult = new VatCrawlResult();
    crawlResult.setVisitId(visitRequest.getVisitId());
    crawlResult.setDomainName(visitRequest.getDomainName());
    crawlResult.setStartUrl(url.toString());
    crawlResult.setCrawlStarted(started);
    crawlResult.setCrawlFinished(finished);
    crawlResult.setVatValues(siteVisit.getVatValues());
    if (siteVisit.getMatchingURL() != null) {
      crawlResult.setMatchingUrl(siteVisit.getMatchingURL().toString());
    } else {
      crawlResult.setMatchingUrl(null);
    }

    List<String> visited = siteVisit.getVisitedURLs().stream().map(HttpUrl::toString).collect(Collectors.toList());
    crawlResult.setVisitedUrls(visited);

    crawlResult.abbreviateData();

    vatCrawlResultRepository.save(crawlResult);
    savePageVisits(visitRequest, siteVisit);
    logger.info("visitId={} domain={} vat={}", crawlResult.getVisitId(), crawlResult.getDomainName(), crawlResult.getVatValues());
  }

  private void savePageVisits(VisitRequest visitRequest, SiteVisit siteVisit) {
    logger.debug("Persisting the {} page visits for {}", siteVisit.getNumberOfVisitedPages(), siteVisit.getBaseURL());

    for (Map.Entry<Link, Page> linkPageEntry : siteVisit.getVisitedPages().entrySet()) {
      Page page = linkPageEntry.getValue();

      boolean isLandingPage = page.getUrl().equals(siteVisit.getBaseURL());
      boolean saveLandingPage = (isLandingPage & persistFirstPageVisit);

      if (persistPageVisits || page.isVatFound() || saveLandingPage) {
        boolean includeBodyText = persistBodyText || page.isVatFound() || saveLandingPage;
        PageVisit pageVisit = page.asPageVisit(visitRequest, includeBodyText);
        pageVisit.setLinkText(linkPageEntry.getKey().getText());
        pageVisitRepository.save(pageVisit);
      }
    }
  }

  public void setPersistPageVisits(boolean persistPageVisits) {
    this.persistPageVisits = persistPageVisits;
  }

  public void setPersistFirstPageVisit(boolean persistFirstPageVisit) {
    this.persistFirstPageVisit = persistFirstPageVisit;
  }

  public void setPersistBodyText(boolean persistBodyText) {
    this.persistBodyText = persistBodyText;
  }

  public void setMaxVisitsPerDomain(int maxVisitsPerDomain) {
    this.maxVisitsPerDomain = maxVisitsPerDomain;
  }
}
