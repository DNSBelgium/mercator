package be.dnsbelgium.mercator.wappalyzer;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.vat.domain.Link;
import be.dnsbelgium.mercator.vat.domain.Page;
import be.dnsbelgium.mercator.vat.domain.SiteVisit;
import be.dnsbelgium.mercator.vat.domain.VatScraper;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlRepository;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlResult;
import be.dnsbelgium.mercator.visits.CrawlerModule;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.HttpUrl;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PageResponse;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TechnologAnalyzerWebCrawler implements CrawlerModule<TechnologyAnalyzerWebCrawlResult> {

    private static final Logger logger = getLogger(TechnologAnalyzerWebCrawler.class);

    private final TechnologyAnalyzer technologyAnalyzer;
    private final MeterRegistry meterRegistry;
    private final TechnologyAnalyzerWebCrawlRepository repository;
    private final VatScraper vatScraper;

    @Value("${technology.analyzer.persist.results:true}")
    private boolean persistResults;

    @Autowired
    public TechnologAnalyzerWebCrawler(TechnologyAnalyzer technologyAnalyzer, MeterRegistry meterRegistry,
            TechnologyAnalyzerWebCrawlRepository repository, VatScraper vatScraper) {
        this.technologyAnalyzer = technologyAnalyzer;
        this.meterRegistry = meterRegistry;
        this.repository = repository;
        this.vatScraper = vatScraper;
    }

    @Override
    public List<TechnologyAnalyzerWebCrawlResult> collectData(VisitRequest visitRequest) {
        Threads.TECHNOLOGY_ANALYZER.incrementAndGet();
        try {
            String url = "https://" + visitRequest.getDomainName();
            HttpUrl httpUrl = HttpUrl.parse(url);
            logger.debug("Visiting {}", httpUrl);

            SiteVisit siteVisit = vatScraper.visit(httpUrl, 10);

            Map<Link, Page> pages = siteVisit.getVisitedPages();

            List<PageResponse> pageResponses = new ArrayList<>();

            for (Page page : pages.values()) {
                var html = page.getDocument().html();
                var headers = page.getHeaders();
                var status = page.getStatusCode();

                Map<String, List<String>> convertedHeaders = new HashMap<>();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    convertedHeaders.put(entry.getKey(), List.of(entry.getValue()));
                }

                PageResponse resp = new PageResponse(status, convertedHeaders, html);

                logger.debug("page.url = {}", page.getUrl());
                pageResponses.add(resp);

            }

            Set<String> detectedTechnologies = technologyAnalyzer.analyze(pageResponses);

            logger.debug("Detected technologies for {}: {}", visitRequest.getDomainName(), detectedTechnologies);

            TechnologyAnalyzerWebCrawlResult webCrawlResult = TechnologyAnalyzerWebCrawlResult.builder()
                    .visitId(visitRequest.getVisitId())
                    .domainName(visitRequest.getDomainName())
                    .detectedTechnologies(detectedTechnologies)
                    .build();

            logger.debug("Detected technologies for {}: {}", visitRequest.getDomainName(), detectedTechnologies);
            meterRegistry.counter("technology.analyzer.crawls.done").increment();

            logger.debug("Saving the detected technologies for {}", webCrawlResult.getDomainName());

            return List.of(webCrawlResult);
        } finally {
            Threads.TECHNOLOGY_ANALYZER.decrementAndGet();
        }
    }

    @Override
    public void save(List<?> collectedData) {
        if (persistResults) {   
            collectedData.forEach(this::saveObject);
        }
    }

    public void saveObject(Object object) {

        if (object instanceof TechnologyAnalyzerWebCrawlResult webCrawlResult) {
            saveItem(webCrawlResult);
        } else {
            logger.error("Cannot save {}", object);
        }
    }

    @Override
    public void saveItem(TechnologyAnalyzerWebCrawlResult webCrawlResult) {
        repository.saveTechnologyAnalyzerWebCrawlResult(webCrawlResult);
        logger.debug("Persisting the detected technologies for {}", webCrawlResult.getDomainName());
    }

    @Override
    public void afterSave(List<?> collectedData) {
    }

    @Override
    public List<TechnologyAnalyzerWebCrawlResult> find(String visitId) {
        return repository.findTechnologyAnalyzerWebCrawlResults(visitId);
    }

    @Override
    public void createTables() {
        logger.debug("creating tables for TechnologyAnalyzerWebCrawlResult");
        repository.createTables();
    }
}