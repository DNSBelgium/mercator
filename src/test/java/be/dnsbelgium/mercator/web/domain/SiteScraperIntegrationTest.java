package be.dnsbelgium.mercator.web.domain;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class SiteScraperIntegrationTest {

   /*
    This test depends on internet access since it tries to access real websites
    It's main purpose is debugging edge cases.
    Ideally the edge cases become proper tests that can run locally
   */

    private SiteScraper siteScraper;
    private static final Logger logger = getLogger(SiteScraperIntegrationTest.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    public void init() {
        PageFetcher pageFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.defaultConfig());
        LinkPrioritizer linkPrioritizer = new ExternalLinkPrioritizer();
        siteScraper = new SiteScraper(pageFetcher, linkPrioritizer, meterRegistry);
    }

    private SiteVisit scrapeForReal(String link) {
        HttpUrl url = HttpUrl.get(link);
        SiteVisit visit = siteScraper.visit(url, 10);
        for (Map.Entry<Link, Page> visitedPage : visit.getVisitedPages().entrySet()) {
            logger.info("we visited link text=[{}] url= {} referer = {}",
                    visitedPage.getKey().getText(),
                    visitedPage.getKey().getUrl(),
                    visitedPage.getKey().getReferer()
            );
            var finalUrl = visitedPage.getValue().getFinalUrl();
            logger.info("finalUrl={}", finalUrl);


        }
        logger.info("visit = {}", visit);
        return visit;
    }

    @Test
    @DisplayName("https://www.dnsbelgium.be")
    public void dnsbelgium() {
        SiteVisit siteVisit = scrapeForReal("https://www.dnsbelgium.be/");
        logger.info("siteVisit = {}", siteVisit);
    }

    @Test
    @DisplayName("https://www.horecacomeback.be")
    public void horecacomeback() {
        SiteVisit siteVisit = scrapeForReal("https://horecacomeback.be/");
        logger.info("siteVisit.getNumberOfVisitedPages= {}", siteVisit.getNumberOfVisitedPages());
    }


}
