package be.dnsbelgium.mercator.vat.wappalyzer;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.vat.WebCrawler;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CrawlDetectedTechnologiesTest {

    private final Logger logger = LoggerFactory.getLogger(CrawlDetectedTechnologiesTest.class);


    @Autowired
    WebCrawler webCrawler;

    @Test
    @Disabled // because it makes a request to test the actual crawler for detecting technologies
    public void testCrawlDetectedTechnologies_setsDetectedTechnologiesToWebCrawlResult() {
        VisitRequest visitRequest = new VisitRequest("abcd-efgh-ijkl-123", "dnsbelgium.be");

        WebCrawlResult webCrawlResult = webCrawler.crawl(visitRequest);
        logger.info("Detected technologies for {}: are: {}", visitRequest.getDomainName(), webCrawlResult.getDetectedTechnologies());
        assertThat(webCrawlResult.getDetectedTechnologies().contains(List.of("Caddy", "HSTS", "Imperva", "Go")));
    }
}
