package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.vat.domain.*;
import be.dnsbelgium.mercator.vat.wappalyzer.TechnologyAnalyzer;
import be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.PageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static be.dnsbelgium.mercator.vat.metrics.MetricName.COUNTER_WEB_CRAWLS_DONE;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class WebCrawler {

    private static final Logger logger = getLogger(WebCrawler.class);

    private final VatScraper vatScraper;
    private final MeterRegistry meterRegistry;
    private final HtmlFeatureExtractor htmlFeatureExtractor;
    private final TechnologyAnalyzer technologyAnalyzer;

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
    public WebCrawler(VatScraper vatScraper, MeterRegistry meterRegistry, HtmlFeatureExtractor htmlFeatureExtractor, TechnologyAnalyzer technologyAnalyzer) {
        this.vatScraper = vatScraper;
        this.meterRegistry = meterRegistry;
        this.htmlFeatureExtractor = htmlFeatureExtractor;
        logger.info("maxVisitsPerDomain = {}", maxVisitsPerDomain);
        this.technologyAnalyzer = technologyAnalyzer;
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

    public PageVisit findSecurityTxt(VisitRequest visitRequest) {
        String domainName = visitRequest.getDomainName();
        logger.debug("Finding robots.txt for {}", domainName);
        String url1 = "https://www.%s/.well-known/security.txt".formatted(domainName);
        String url2 = "https://%s/.well-known/security.txt".formatted(domainName);
        PageVisit pageVisit = find(url1, url2, visitRequest);
        pageVisit.clearHtml();
        return pageVisit;
    }

    public PageVisit findRobotsTxt(VisitRequest visitRequest) {
        String domainName = visitRequest.getDomainName();
        logger.debug("Finding security.txt for {}", domainName);
        String url1 = "https://www.%s/robots.txt".formatted(domainName);
        String url2 = "https://%s/robots.txt".formatted(domainName);
        PageVisit pageVisit = find(url1, url2, visitRequest);
        pageVisit.clearHtml();
        return pageVisit;
    }

    public PageVisit find(String url1, String url2, VisitRequest visitRequest) {
        Page page1 = vatScraper.fetchAndParse(HttpUrl.parse(url1));
        if (page1 != null && page1.getStatusCode() == 200) {
            return page1.asPageVisit(visitRequest, true);
        }
        Page page2 = vatScraper.fetchAndParse(HttpUrl.parse(url2));
        if (page2 != null && page2.getStatusCode() == 200) {
            return page2.asPageVisit(visitRequest, true);
        }
        if (page1 != null) {
            return page1.asPageVisit(visitRequest, true);
        }
        return null;
    }

    public WebCrawlResult crawl(VisitRequest visitRequest) {
        SiteVisit siteVisit = this.visit(visitRequest);
        WebCrawlResult webCrawlResult = this.convert(visitRequest, siteVisit);
        meterRegistry.counter(COUNTER_WEB_CRAWLS_DONE).increment();
        List<HtmlFeatures> featuresList = findFeatures(visitRequest, siteVisit);
        webCrawlResult.setHtmlFeatures(featuresList);
        List<PageVisit> pageVisits = new ArrayList<>();
        List<PageResponse> pageResponses = new ArrayList<>();
        logger.info(siteVisit.getBaseURL().toString());
        for (Map.Entry<Link, Page> linkPageEntry : siteVisit.getVisitedPages().entrySet()) {
            Page page = linkPageEntry.getValue();
            boolean includeBodyText = false;
            PageVisit pageVisit = page.asPageVisit(visitRequest, includeBodyText);
            pageVisit.setLinkText(linkPageEntry.getKey().getText());
            pageVisits.add(pageVisit);

            // integrated wappalyzer
            String html = page.getDocument().html();
            Map<String, String> headers = page.getHeaders();
            int status = page.getStatusCode();
            Map<String, List<String>> convertedHeaders = new HashMap<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                convertedHeaders.put(entry.getKey(), List.of(entry.getValue()));
            }
            PageResponse resp = new PageResponse(status, convertedHeaders, html);
            pageResponses.add(resp);
        }

        PageVisit robotsTxtVisit = findRobotsTxt(visitRequest);

        if (robotsTxtVisit != null) {
            pageVisits.add(robotsTxtVisit);
        }

        PageVisit securityTxtVisit = findSecurityTxt(visitRequest);
        if (securityTxtVisit != null) {
            pageVisits.add(securityTxtVisit);
        }
        Set<String> detectedTechnologies = technologyAnalyzer.analyze(pageResponses);
        logger.debug("detectedTechnologies = {}", detectedTechnologies);

        // integrated wappalyzer
        webCrawlResult.setDetectedTechnologies(detectedTechnologies);
        webCrawlResult.setPageVisits(pageVisits);
        return webCrawlResult;
    }

}
