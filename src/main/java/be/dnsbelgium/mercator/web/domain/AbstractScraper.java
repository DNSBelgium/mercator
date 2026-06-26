package be.dnsbelgium.mercator.web.domain;

import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;


public abstract class AbstractScraper {

    protected final PageFetcher pageFetcher;
    protected final LinkPrioritizer linkPrioritizer;
    protected final MeterRegistry meterRegistry;

    private static final Logger logger = getLogger(be.dnsbelgium.mercator.web.domain.AbstractScraper.class);

    @Autowired
    public AbstractScraper(MeterRegistry meterRegistry, PageFetcher pageFetcher, LinkPrioritizer linkPrioritizer) {
        this.pageFetcher = pageFetcher;
        this.linkPrioritizer = linkPrioritizer;
        this.meterRegistry = meterRegistry;
    }

    abstract public boolean goalReached(SiteVisit siteVisit);
    abstract public void logGoalReached(HttpUrl url, SiteVisit siteVisit);
    abstract public void updateMetrics(SiteVisit siteVisit);
    abstract public void logResult(HttpUrl url, SiteVisit siteVisit);
    abstract public void findMoreLinksToVisit(Page page, Link link, SiteVisit siteVisit);

    protected void updateGoal(Page page) {
    }

    public SiteVisit visit(HttpUrl url, int maxVisits) {
        SiteVisit siteVisit = new SiteVisit(url);
        logger.debug("Analyze landing page {}", url);

        Link startingLink = new Link(url, "");

        // this will either reach our goal (e.g. find a VAT) or add inner links to the queue
        visit(siteVisit, startingLink, true);

        if (!goalReached(siteVisit)) {
            logger.debug("Goal not reached on landing page => crawling inner links");
            while (true) {
                if (siteVisit.getNumberOfVisitedLinks() >= maxVisits) {
                    logger.debug("reached max number of page visits per site for {} => done", url);
                    break;
                }
                if (goalReached(siteVisit)) {
                    logGoalReached(url, siteVisit);
                    break;
                }
                Optional<PrioritizedLink> next = siteVisit.getHighestPriorityLink();
                if (next.isEmpty()) {
                    logger.debug("No more links to follow for {} => done", url);
                    break;
                }
                logger.debug("Visiting {} for {}", next.get(), url);
                visit(siteVisit, next.get(), false);
            }
        }
        updateMetrics(siteVisit);
        logResult(url, siteVisit);
        siteVisit.setFinished(Instant.now());
        return siteVisit;
    }

    private void visit(SiteVisit siteVisit, Link link, boolean isFirstPage)  {
        HttpUrl url = link.getUrl();
        logger.debug("Visiting {}. isFirstPage:  {}", url, isFirstPage);

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
            siteVisit.add(link, page);
            return;
        }
        if (isFirstPage) {
            siteVisit.setLandingURL(page.getFinalUrl());
            logger.info("siteVisit.getLandingURL = {}", siteVisit.getLandingURL());
        }
        updateGoal(page);
        siteVisit.markVisited(link);
        siteVisit.add(link, page);

        if (!goalReached(siteVisit)) {
            findMoreLinksToVisit(page, link, siteVisit);
        }
    }

    public Page fetchAndParse(HttpUrl url) {
        try {
            return pageFetcher.fetch(url);
        } catch (Exception e) {
            logger.debug("Failed to fetch {} because of {}", url, e.getMessage());
            return null;
        }
    }


}
