package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.vat.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InterruptedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VatScraper {

  private final PageFetcher pageFetcher;
  private final VatFinder vatFinder;
  private final LinkPrioritizer linkPrioritizer;
  private final MeterRegistry meterRegistry;

  private static final Logger logger = getLogger(VatScraper.class);

  @Autowired
  public VatScraper(MeterRegistry meterRegistry, PageFetcher pageFetcher, VatFinder vatFinder, LinkPrioritizer linkPrioritizer) {
    this.pageFetcher = pageFetcher;
    this.vatFinder = vatFinder;
    this.linkPrioritizer = linkPrioritizer;
    this.meterRegistry = meterRegistry;
  }

  public SiteVisit visit(HttpUrl url, int maxVisits) {
    SiteVisit siteVisit = new SiteVisit(url);
    logger.debug("Analyze landing page {}", url);

    Link startingLink = new Link(url, "");

    // this will either find a VAT or add inner links to the queue
    visit(siteVisit, startingLink);

    if (!siteVisit.isVatFound()) {
      logger.debug("No VAT found on landing page => crawling inner links");
      while (true) {
        Optional<PrioritizedLink> next = siteVisit.getHighestPriorityLink();
        if (next.isEmpty()) {
          logger.debug("No more links to follow for {} => done", url);
          break;
        }
        if (siteVisit.getNumberOfVisitedLinks() >= maxVisits) {
          logger.debug("reached max number of page visits per site for {} => done", url);
          break;
        }
        if (siteVisit.isVatFound()) {
          logger.debug("Found VAT {} for {} after {} visits => done", siteVisit.getVatValues(), url, siteVisit.getNumberOfVisitedPages());
          break;
        }
        logger.debug("Visiting {} for {}", next.get(), url);
        visit(siteVisit, next.get());
      }
    }
    if (siteVisit.isVatFound()) {
      meterRegistry.counter(MetricName.COUNTER_SITES_WITH_VAT).increment();
    } else {
      meterRegistry.counter(MetricName.COUNTER_SITES_WITHOUT_VAT).increment();
    }
    logger.debug("Finished scraping for {} => vatFound = {}.", url, siteVisit.isVatFound());
    siteVisit.setFinished(Instant.now());
    return siteVisit;
  }

  private void visit(SiteVisit siteVisit, Link link)  {
    HttpUrl url = link.getUrl();

    if (siteVisit.alreadyVisited(url)) {
      logger.debug("we already visited {}", url);
      return;
    }
    Page page = fetchAndParse(url);
    if (page == null) {
      logger.debug("No content found on {}", url);
      siteVisit.markVisited(link);
      return;
    }
    if (page.getDocument() == null) {
      logger.debug("Page {} has no document", url);
      siteVisit.markVisited(link);
      return;
    }
    findVAT(page);
    siteVisit.markVisited(link);
    siteVisit.add(link, page);

    if (!page.isVatFound()) {
      Set<Link> innerLinks = page.getInnerLinks();
      for (Link innerLink : innerLinks) {
        PrioritizedLink prioritizedLink = linkPrioritizer.prioritize(innerLink);
        siteVisit.addLinkToVisit(prioritizedLink);
      }
    }
  }

  private void findVAT(Page page) {
    Element body = page.getDocument().body();
      //noinspection ConstantValue
      if (body == null) {
      logger.debug("Document on {} has no body", page.getUrl());
      return;
    }
    String text = page.getDocument().body().text();
    logger.debug("url={} text.length={} ", page.getUrl(), text.length());

    List<String> validVatValues = vatFinder.findValidVatValues(text);
    logger.debug("{} => Found {} valid VAT values", page.getUrl(), validVatValues.size());

    if (!validVatValues.isEmpty()) {
      if (validVatValues.size() > 20) {
        // sometimes binary files or text files have many matches
        logger.warn("Found more than 20 valid VAT values. Probably not reliable => not saving VAT value");
      } else {
        logger.info("{} => valid VAT values {}", page.getUrl(), validVatValues);
        page.setVatValues(validVatValues);
      }
    } else {
      logger.debug("No valid VAT values found");
      // let's show_tables log VAT values with wrong checksum
      List<String> vatLikeValues = vatFinder.findVatValues(text);
      int countVatLikeValues = vatLikeValues.size();
      if (countVatLikeValues > 0 && countVatLikeValues < 3) {
        logger.debug("{} => Found VAT-like values {}", page.getUrl(), vatLikeValues);
      }
    }
  }

  public Page fetchAndParse(HttpUrl url) {
    try {
      return pageFetcher.fetch(url);
    } catch (InterruptedIOException e) {
      return Page.PAGE_TIME_OUT;
    } catch (Exception e) {
      logger.debug("Failed to fetch {} because of {}", url, e.getMessage());
      return null;
    }
  }


}
