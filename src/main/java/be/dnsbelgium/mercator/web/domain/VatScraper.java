package be.dnsbelgium.mercator.web.domain;

import be.dnsbelgium.mercator.web.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VatScraper extends AbstractScraper {

private final VatFinder vatFinder;

  private static final Logger logger = getLogger(VatScraper.class);

  @Autowired
  public VatScraper(MeterRegistry meterRegistry, PageFetcher pageFetcher, VatFinder vatFinder, VatLinkPrioritizer linkPrioritizer) {
    super(meterRegistry, pageFetcher, linkPrioritizer);
    this.vatFinder = vatFinder;
  }

  @Override
  public boolean goalReached(SiteVisit siteVisit) {
    return siteVisit.isVatFound();
  }

  @Override
  public void findMoreLinksToVisit(Page page, Link link, SiteVisit siteVisit) {
    Set<Link> innerLinks = page.getInnerLinks();
    for (Link innerLink : innerLinks) {
      PrioritizedLink prioritizedLink = linkPrioritizer.prioritize(siteVisit, page, innerLink);
      siteVisit.addLinkToVisit(prioritizedLink);
    }
  }

  @Override
  public void logGoalReached(HttpUrl url, SiteVisit siteVisit) {
    logger.debug("Found VAT {} for {} after {} visits => done", siteVisit.getVatValues(), url, siteVisit.getNumberOfVisitedPages());
  }

  @Override
  public void logResult(HttpUrl url, SiteVisit siteVisit) {
    logger.debug("Finished scraping for {} => vatFound = {}.", url, siteVisit.isVatFound());
  }

  @Override
  public void updateMetrics(SiteVisit siteVisit) {
    if (siteVisit.isVatFound()) {
      meterRegistry.counter(MetricName.COUNTER_SITES_WITH_VAT).increment();
    } else {
      meterRegistry.counter(MetricName.COUNTER_SITES_WITHOUT_VAT).increment();
    }
  }

  @Override
  protected void updateGoal(Page page) {
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
        if (validVatValues.size() <= 20) {
          logger.info("{} => valid VAT values {}", page.getUrl(), validVatValues);
          page.setVatValues(validVatValues);
        } else {
          // sometimes binary files or text files have many matches
          logger.debug("Found more than 20 valid VAT values. Probably not reliable => not saving VAT value");
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

}
