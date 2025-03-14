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
import java.lang.instrument.Instrumentation;

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

    public PageVisit findSecurityTxt(HttpUrl baseURL, VisitRequest visitRequest) {
        List<String> securityTxtUrls = List.of(
                "https://www." + baseURL.host() + "/.well-known/security.txt",
                "https://" + baseURL.host() + "/.well-known/security.txt"
        );

        logger.info("Using following urls: {} ", securityTxtUrls);

        for (String securityTxtUrl : securityTxtUrls) {
            Page securityTxtPage = vatScraper.fetchAndParse(HttpUrl.parse(securityTxtUrl));

            if (securityTxtPage == null || securityTxtPage.getStatusCode() == 404) {
                continue;
            }

            byte[] responseBytes = securityTxtPage.getResponseBody().getBytes();
            int responseSize = responseBytes.length;

            if (responseSize > 32000) {
                logger.info("Security.txt file too large: {} bytes", responseSize);
                return null;
            }

            PageVisit securityTxtVisit = securityTxtPage.asPageVisit(visitRequest, false);
            securityTxtVisit.setSecurity_txt_url(securityTxtPage.getUrl().toString());
            securityTxtVisit.setSecurity_txt_response_headers(securityTxtPage.getHeaders());
            securityTxtVisit.setSecurity_txt_bytes(responseSize);
            securityTxtVisit.setSecurity_txt_content(securityTxtPage.getDocument().text()); // Store content

            return securityTxtVisit;
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
        // create pagevisit for security.txt seperate from other pagevisits, so it is stored once
        PageVisit securityTxtVisit = findSecurityTxt(siteVisit.getBaseURL(), visitRequest);
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
