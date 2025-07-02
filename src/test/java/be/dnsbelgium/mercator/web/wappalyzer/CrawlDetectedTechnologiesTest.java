package be.dnsbelgium.mercator.web.wappalyzer;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.web.domain.Page;
import be.dnsbelgium.mercator.web.domain.PageFetcher;
import be.dnsbelgium.mercator.web.domain.PageFetcherConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CrawlDetectedTechnologiesTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final Logger logger = LoggerFactory.getLogger(CrawlDetectedTechnologiesTest.class);

    @Test
    @EnabledIfEnvironmentVariable(named = "WEB_OUTBOUND_TEST_ENABLED", matches = "true")
    public void testCrawlDetectedTechnologies_setsDetectedTechnologiesToWebCrawlResult() throws IOException {
        VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), "dnsbelgium.be");

        PageFetcher pageFetcher = new PageFetcher(new SimpleMeterRegistry(), PageFetcherConfig.defaultConfig());
        Page page = pageFetcher.fetch(HttpUrl.get("https://www.dnsbelgium.be/"));

        TechnologyAnalyzer technologyAnalyzer = new TechnologyAnalyzer(meterRegistry);
        Set<String> technologies = technologyAnalyzer.analyze(page);

        logger.info("Detected technologies for {}: are: {}", visitRequest.getDomainName(), technologies);
        assertThat(technologies).contains("Open Graph", "HSTS", "Imperva", "PWA");
    }
}
