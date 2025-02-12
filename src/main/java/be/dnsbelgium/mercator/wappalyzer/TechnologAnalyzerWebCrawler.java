package be.dnsbelgium.mercator.wappalyzer;

import be.dnsbelgium.mercator.common.VisitRequest;
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

    @Value("${technology.analyzer.persist.results:false}")
    private boolean persistResults;

    @Autowired
    public TechnologAnalyzerWebCrawler(TechnologyAnalyzer technologyAnalyzer, MeterRegistry meterRegistry) {
        this.technologyAnalyzer = technologyAnalyzer;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<TechnologyAnalyzerWebCrawlResult> collectData(VisitRequest visitRequest) {
        String url = "https://" + visitRequest.getDomainName();
        Set<String> detectedTechnologies = technologyAnalyzer.analyze(url);
        logger.info("Detected technologies for {}: {}", visitRequest.getDomainName(), detectedTechnologies);

        TechnologyAnalyzerWebCrawlResult webCrawlResult = TechnologyAnalyzerWebCrawlResult.builder()
                .visitId(visitRequest.getVisitId())
                .domainName(visitRequest.getDomainName())
                .detectedTechnologies(detectedTechnologies)
                .build();

                // DEBUG log voor te zien of da werkt
        logger.info( "Detected technologies for {}: {}", visitRequest.getDomainName(), detectedTechnologies);
        meterRegistry.counter("technology.analyzer.crawls.done").increment();

        return List.of(webCrawlResult);
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
        
        logger.debug("Persisting the detected technologies for {}", webCrawlResult.getDomainName());
    }

    @Override
    public void afterSave(List<?> collectedData) {
        // placehoder?
    }

    @Override
    public List<TechnologyAnalyzerWebCrawlResult> find(String visitId) {
        // placeholder?
        return List.of(); 
    }

    @Override
    public void createTables() {
        // placeholder?
    }
}