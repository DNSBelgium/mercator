package be.dnsbelgium.mercator.wappalyzer;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlRepository;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TechnologyAnalyzerWebCrawlerTest {

    private TechnologyAnalyzer technologyAnalyzer;
    private MeterRegistry meterRegistry;
    private TechnologyAnalyzerWebCrawlRepository repository;
    private TechnologAnalyzerWebCrawler webCrawler;

    @Before
    public void setUp() {
        technologyAnalyzer = mock(TechnologyAnalyzer.class);
        meterRegistry = mock(MeterRegistry.class);
        repository = mock(TechnologyAnalyzerWebCrawlRepository.class);
        webCrawler = new TechnologAnalyzerWebCrawler(technologyAnalyzer, meterRegistry, repository, null);

        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    @Test
    public void givenCollectedData_whenSave_thenPersistResults() {
        TechnologyAnalyzerWebCrawlResult result = TechnologyAnalyzerWebCrawlResult.builder()
                .visitId("01JKZDXS4QFF16YAF8A4KR4RS1")
                .domainName("wikipedia.org")
                .detectedTechnologies(Set.of("Apache Traffic Server", "HSTS", "Open Graph"))
                .build();

        webCrawler.save(List.of(result));

        when(webCrawler.find("01JKZDXS4QFF16YAF8A4KR4RS1")).thenReturn(List.of(result));

        List<TechnologyAnalyzerWebCrawlResult> results = webCrawler.find("01JKZDXS4QFF16YAF8A4KR4RS1");

        assertThat(results).hasSize(1);
        TechnologyAnalyzerWebCrawlResult savedResult = results.get(0);
        assertThat(savedResult.getDetectedTechnologies()).containsExactlyInAnyOrder("Apache Traffic Server", "HSTS",
                "Open Graph");
    }
}