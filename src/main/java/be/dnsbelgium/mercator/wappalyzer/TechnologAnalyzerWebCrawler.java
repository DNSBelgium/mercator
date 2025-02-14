package be.dnsbelgium.mercator.wappalyzer;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlRepository;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlResult;
import be.dnsbelgium.mercator.visits.CrawlerModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TechnologAnalyzerWebCrawler implements CrawlerModule<TechnologyAnalyzerWebCrawlResult> {

    private static final Logger logger = getLogger(TechnologAnalyzerWebCrawler.class);

    private final TechnologyAnalyzer technologyAnalyzer;
    private final MeterRegistry meterRegistry;
    private final TechnologyAnalyzerWebCrawlRepository repository;

    @Value("${technology.analyzer.persist.results:true}")
    private boolean persistResults;

    @Autowired
    public TechnologAnalyzerWebCrawler(TechnologyAnalyzer technologyAnalyzer, MeterRegistry meterRegistry,
            TechnologyAnalyzerWebCrawlRepository repository) {
        this.technologyAnalyzer = technologyAnalyzer;
        this.meterRegistry = meterRegistry;
        this.repository = repository;
    }

    @Override
    public List<TechnologyAnalyzerWebCrawlResult> collectData(VisitRequest visitRequest) {
        Threads.TECHNOLOGY_ANALYZER.incrementAndGet();
        try {
            String url = "https://" + visitRequest.getDomainName();
            Set<String> detectedTechnologies = technologyAnalyzer.analyze(url);
            logger.info("Detected technologies for {}: {}", visitRequest.getDomainName(), detectedTechnologies);

            TechnologyAnalyzerWebCrawlResult webCrawlResult = TechnologyAnalyzerWebCrawlResult.builder()
                    .visitId(visitRequest.getVisitId())
                    .domainName(visitRequest.getDomainName())
                    .detectedTechnologies(detectedTechnologies)
                    .build();

            logger.info("Detected technologies for {}: {}", visitRequest.getDomainName(), detectedTechnologies);
            meterRegistry.counter("technology.analyzer.crawls.done").increment();

            logger.info("Saving the detected technologies for {}", webCrawlResult.getDomainName());
            save(List.of(webCrawlResult));

            return List.of(webCrawlResult);
        } finally {
            Threads.TECHNOLOGY_ANALYZER.decrementAndGet();
        }
    }

    @Override
    public void save(List<?> collectedData) {
        logger.info("step 1");
        if (persistResults) {
            logger.info("condition persistresults was true here");
            collectedData.forEach(this::saveObject);
        }
    }

    public void saveObject(Object object) {
        logger.info("step 3");

        if (object instanceof TechnologyAnalyzerWebCrawlResult webCrawlResult) {
            logger.info("it wa instance, saving... ");
            saveItem(webCrawlResult);
        } else {
            logger.error("Cannot save {}", object);
        }
    }

    @Override
    public void saveItem(TechnologyAnalyzerWebCrawlResult webCrawlResult) {
        logger.info("step 4 actual save method");
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
        logger.info("creating tables for TechnologyAnalyzerWebCrawlResult");
        repository.createTables();
    }
}