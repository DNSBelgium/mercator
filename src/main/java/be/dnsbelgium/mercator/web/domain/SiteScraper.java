package be.dnsbelgium.mercator.web.domain;

import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.HttpUrl;
import org.slf4j.Logger;

import java.util.Set;

import static be.dnsbelgium.mercator.web.domain.Page.getSecondLevelDomainName;
import static org.slf4j.LoggerFactory.getLogger;

public class SiteScraper extends AbstractScraper {

    private static final Logger logger = getLogger(SiteScraper.class);

    public SiteScraper(PageFetcher pageFetcher, LinkPrioritizer linkPrioritizer, MeterRegistry meterRegistry) {
        super(meterRegistry, pageFetcher, linkPrioritizer);
    }

    @Override
    public boolean goalReached(SiteVisit siteVisit) {
        // we keep crawling until max visits is reached.
        return false;
    }

    @Override
    public void logGoalReached(HttpUrl url, SiteVisit siteVisit) {
      // nothing to do
    }

    @Override
    public void updateMetrics(SiteVisit siteVisit) {
    }

    @Override
    public void logResult(HttpUrl url, SiteVisit siteVisit) {
        logger.debug("we visited {} pages for {}", siteVisit.getNumberOfVisitedPages(), url);
    }


    @Override
    public void findMoreLinksToVisit(Page page, Link previousLink, SiteVisit siteVisit) {
        String landingDomainName = getSecondLevelDomainName(siteVisit.getLandingURL());
        String currentDomainName = getSecondLevelDomainName(page.getUrl());

        // only search internal/external links when we are on an internal page
        if (currentDomainName.equals(landingDomainName)) {
            Set<Link> links = page.getLinks();
            logger.debug("Found {} links on {}", links.size(), page.getUrl());
            for (Link link : links) {
                var prioritizedLink = linkPrioritizer.prioritize(siteVisit, page, link);
                siteVisit.addLinkToVisit(prioritizedLink);
            }
        }

    }

}
