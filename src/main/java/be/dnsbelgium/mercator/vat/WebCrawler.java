package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisit;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebCrawlResult;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebRepository;
import be.dnsbelgium.mercator.vat.domain.Link;
import be.dnsbelgium.mercator.vat.domain.Page;
import be.dnsbelgium.mercator.vat.domain.SiteVisit;
import be.dnsbelgium.mercator.vat.domain.VatScraper;
import eu.bosteels.mercator.mono.metrics.Threads;
import eu.bosteels.mercator.mono.visits.CrawlerModule;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static be.dnsbelgium.mercator.vat.metrics.MetricName.COUNTER_WEB_CRAWLS_DONE;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class WebCrawler implements CrawlerModule<WebCrawlResult> {

    private static final Logger logger = getLogger(WebCrawler.class);

    private final VatScraper vatScraper;
    private final MeterRegistry meterRegistry;
    private final HtmlFeatureExtractor htmlFeatureExtractor;
    private final WebRepository webRepository;

    @Setter
    @Value("${vat.crawler.max.visits.per.domain:10}")
    private int maxVisitsPerDomain = 10;

    @Setter
    @Value("${vat.crawler.persist.page.visits:false}")
    private boolean persistPageVisits = false;

    @Setter
    @Value("${vat.crawler.persist.first.page.visit:false}")
    private boolean persistFirstPageVisit = false;

    @Setter
    @Value("${vat.crawler.persist.body.text:false}")
    private boolean persistBodyText = false;

    @Autowired
    public WebCrawler(VatScraper vatScraper, MeterRegistry meterRegistry, HtmlFeatureExtractor htmlFeatureExtractor, WebRepository webRepository) {
        this.vatScraper = vatScraper;
        this.meterRegistry = meterRegistry;
        this.htmlFeatureExtractor = htmlFeatureExtractor;
        this.webRepository = webRepository;
    }

    @PostConstruct
    public void init() {
        logger.info("maxVisitsPerDomain={}", maxVisitsPerDomain);
        logger.info("persistPageVisits={}", persistPageVisits);
        logger.info("persistBodyText={}", persistBodyText);
        logger.info("persistFirstPageVisit={}", persistFirstPageVisit);
    }

    public SiteVisit visit(VisitRequest visitRequest) {
        Threads.WEB.incrementAndGet();
        try {
            return findVatValues(visitRequest);
        } finally {
            Threads.WEB.decrementAndGet();
        }
    }

    public SiteVisit findVatValues(VisitRequest visitRequest) {
        String fqdn = visitRequest.getDomainName();
        logger.debug("Searching VAT info for domainName={} and visitId={}", fqdn, visitRequest.getVisitId());

        String startURL = "http://www." + fqdn;

        HttpUrl url = HttpUrl.parse(startURL);

        if (url == null) {
            // this is probably a bug: log + throw
            logger.error("VisitRequest {} => invalid URL: {} ", visitRequest, startURL);
            String message = String.format("visitRequest %s lead to invalid url: null", visitRequest);
            throw new RuntimeException(message);
        }

        SiteVisit siteVisit = vatScraper.visit(url, maxVisitsPerDomain);
        logger.debug("siteVisit = {}", siteVisit);

        logger.info("visitId={} domain={} vat={}", visitRequest.getVisitId(), visitRequest.getDomainName(), siteVisit.getVatValues());
        return siteVisit;
    }

    public WebCrawlResult convert(VisitRequest visitRequest, SiteVisit siteVisit) {
        String matchingUrl = (siteVisit.getMatchingURL() != null) ? siteVisit.getMatchingURL().toString() : null;
        List<String> visited = siteVisit.getVisitedURLs()
                .stream()
                .map(HttpUrl::toString)
                .collect(Collectors.toList());
        WebCrawlResult crawlResult = WebCrawlResult.builder()
                .visitId(visitRequest.getVisitId())
                .domainName(visitRequest.getDomainName())
                .startUrl(siteVisit.getBaseURL().toString())
                .crawlStarted(siteVisit.getStarted())
                .crawlFinished(siteVisit.getFinished())
                .vatValues(siteVisit.getVatValues())
                .matchingUrl(matchingUrl)
                .visitedUrls(visited)
                .build();

        crawlResult.abbreviateData();
        return crawlResult;
    }

    private List<HtmlFeatures> findFeatures(VisitRequest visitRequest, SiteVisit siteVisit) {
        Threads.FEATURE_EXTRACTION.incrementAndGet();
        try {
            logger.info("siteVisit = {}", siteVisit);
            List<HtmlFeatures> featuresList = new ArrayList<>();
            for (Page page : siteVisit.getVisitedPages().values()) {
                var html = page.getDocument().html();
                logger.info("page.url = {}", page.getUrl());
                var features = htmlFeatureExtractor.extractFromHtml(
                        html,
                        page.getUrl().url().toExternalForm(),
                        visitRequest.getDomainName()
                );
                features.visitId = visitRequest.getVisitId();
                features.crawlTimestamp = Instant.now();
                features.domainName = visitRequest.getDomainName();
                featuresList.add(features);
            }
            return featuresList;
        } finally {
            Threads.FEATURE_EXTRACTION.decrementAndGet();
        }
    }

    @Override
    public List<WebCrawlResult> collectData(VisitRequest visitRequest) {
        SiteVisit siteVisit = this.visit(visitRequest);
        WebCrawlResult webCrawlResult = this.convert(visitRequest, siteVisit);
        meterRegistry.counter(COUNTER_WEB_CRAWLS_DONE).increment();
        List<HtmlFeatures> featuresList = findFeatures(visitRequest, siteVisit);
        webCrawlResult.setHtmlFeatures(featuresList);
        List<PageVisit> pageVisits = new ArrayList<>();
        for (Map.Entry<Link, Page> linkPageEntry : siteVisit.getVisitedPages().entrySet()) {
            Page page = linkPageEntry.getValue();
            boolean includeBodyText = false;
            PageVisit pageVisit = page.asPageVisit(visitRequest, includeBodyText);
            pageVisit.setLinkText(linkPageEntry.getKey().getText());
            pageVisits.add(pageVisit);
        }
        webCrawlResult.setPageVisits(pageVisits);
        return List.of(webCrawlResult);
    }

    @Override
    public void save(List<?> collectedData) {
        collectedData.forEach(this::saveObject);
    }

    public void saveObject(Object object) {
        if (object instanceof WebCrawlResult webCrawlResult) {
            saveItem(webCrawlResult);
        } else {
            logger.error("Cannot save {}", object);
        }
    }

    @Override
    public void saveItem(WebCrawlResult webCrawlResult) {
        webRepository.saveWebVisit(webCrawlResult);
        logger.debug("Persisting the {} page visits for {}",
                webCrawlResult.getPageVisits().size(), webCrawlResult.getStartUrl());
        webRepository.savePageVisits(webCrawlResult.getPageVisits());
        for (HtmlFeatures htmlFeatures : webCrawlResult.getHtmlFeatures()) {
            webRepository.save(htmlFeatures);
        }
    }

    @Override
    public void afterSave(List<?> collectedData) {
        // nothing to do
    }

    @Override
    public List<WebCrawlResult> find(String visitId) {
        return webRepository.findWebCrawlResult(visitId);
    }

    @Override
    public void createTables() {
        webRepository.createTables();
    }
}
