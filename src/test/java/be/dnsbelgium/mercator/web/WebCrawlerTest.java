package be.dnsbelgium.mercator.web;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.web.domain.AbstractScraper;
import be.dnsbelgium.mercator.web.domain.Link;
import be.dnsbelgium.mercator.web.domain.Page;
import be.dnsbelgium.mercator.web.domain.SiteVisit;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import be.dnsbelgium.mercator.web.wappalyzer.TechnologyAnalyzer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebCrawlerTest {

    @Test
    void crawlKeepsResultWhenTechnologyDetectionOverflows() {
        VisitRequest visitRequest = new VisitRequest("visit-id", "example.be");
        HttpUrl url = HttpUrl.get("https://www.example.be/");
        Page page = Page.builder()
                .url(url)
                .visitStarted(Instant.now())
                .visitFinished(Instant.now())
                .statusCode(200)
                .responseBody("<html><body>Example</body></html>")
                .headers(Map.of())
                .build();
        SiteVisit siteVisit = new SiteVisit(url);
        siteVisit.add(new Link(url, ""), page);

        AbstractScraper scraper = mock(AbstractScraper.class);
        when(scraper.visit(HttpUrl.get("http://www.example.be"), 10)).thenReturn(siteVisit);
        TechnologyAnalyzer technologyAnalyzer = mock(TechnologyAnalyzer.class);
        when(technologyAnalyzer.analyze(page)).thenThrow(new StackOverflowError("simulated regex overflow"));

        WebCrawler crawler = new WebCrawler(
                scraper,
                new SimpleMeterRegistry(),
                mock(HtmlFeatureExtractor.class),
                technologyAnalyzer);

        WebCrawlResult result = crawler.crawl(visitRequest);

        assertThat(result.getVisitId()).isEqualTo("visit-id");
        assertThat(result.getPageVisits()).hasSize(1);
        assertThat(result.getPageVisits().getFirst().getDetectedTechnologies()).isEmpty();
    }

    @Test
    void crawlKeepsResultWhenTechnologyDetectionFails() {
        VisitRequest visitRequest = new VisitRequest("visit-id", "example.be");
        HttpUrl url = HttpUrl.get("https://www.example.be/");
        Page page = Page.builder()
                .url(url)
                .visitStarted(Instant.now())
                .visitFinished(Instant.now())
                .statusCode(200)
                .responseBody("<html><body>Example</body></html>")
                .headers(Map.of())
                .build();
        SiteVisit siteVisit = new SiteVisit(url);
        siteVisit.add(new Link(url, ""), page);

        AbstractScraper scraper = mock(AbstractScraper.class);
        when(scraper.visit(HttpUrl.get("http://www.example.be"), 10)).thenReturn(siteVisit);
        TechnologyAnalyzer technologyAnalyzer = mock(TechnologyAnalyzer.class);
        when(technologyAnalyzer.analyze(page)).thenThrow(new IllegalArgumentException("invalid technology input"));

        WebCrawler crawler = new WebCrawler(
                scraper,
                new SimpleMeterRegistry(),
                mock(HtmlFeatureExtractor.class),
                technologyAnalyzer);

        WebCrawlResult result = crawler.crawl(visitRequest);

        assertThat(result.getVisitId()).isEqualTo("visit-id");
        assertThat(result.getPageVisits()).hasSize(1);
        assertThat(result.getPageVisits().getFirst().getDetectedTechnologies()).isEmpty();
    }

    @Test
    void crawlKeepsResultWhenHtmlFeatureExtractionFails() {
        VisitRequest visitRequest = new VisitRequest("visit-id", "example.be");
        HttpUrl url = HttpUrl.get("https://www.example.be/");
        Page page = Page.builder()
                .url(url)
                .visitStarted(Instant.now())
                .visitFinished(Instant.now())
                .statusCode(200)
                .responseBody("<html><body>Example</body></html>")
                .headers(Map.of())
                .build();
        SiteVisit siteVisit = new SiteVisit(url);
        siteVisit.add(new Link(url, ""), page);

        AbstractScraper scraper = mock(AbstractScraper.class);
        when(scraper.visit(HttpUrl.get("http://www.example.be"), 10)).thenReturn(siteVisit);
        HtmlFeatureExtractor htmlFeatureExtractor = mock(HtmlFeatureExtractor.class);
        when(htmlFeatureExtractor.extractFromHtml(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("invalid HTML input"));

        WebCrawler crawler = new WebCrawler(
                scraper,
                new SimpleMeterRegistry(),
                htmlFeatureExtractor,
                mock(TechnologyAnalyzer.class));

        WebCrawlResult result = crawler.crawl(visitRequest);

        assertThat(result.getVisitId()).isEqualTo("visit-id");
        assertThat(result.getPageVisits()).hasSize(1);
        assertThat(result.getPageVisits().getFirst().getHtmlFeatures()).isNull();
    }

    @Test
    void crawlKeepsResultWhenHtmlFeatureExtractionOverflows() {
        VisitRequest visitRequest = new VisitRequest("visit-id", "example.be");
        HttpUrl url = HttpUrl.get("https://www.example.be/");
        Page page = Page.builder()
                .url(url)
                .visitStarted(Instant.now())
                .visitFinished(Instant.now())
                .statusCode(200)
                .responseBody("<html><body>Example</body></html>")
                .headers(Map.of())
                .build();
        SiteVisit siteVisit = new SiteVisit(url);
        siteVisit.add(new Link(url, ""), page);

        AbstractScraper scraper = mock(AbstractScraper.class);
        when(scraper.visit(HttpUrl.get("http://www.example.be"), 10)).thenReturn(siteVisit);
        HtmlFeatureExtractor htmlFeatureExtractor = mock(HtmlFeatureExtractor.class);
        when(htmlFeatureExtractor.extractFromHtml(anyString(), anyString(), anyString()))
                .thenThrow(new StackOverflowError("simulated regex overflow"));

        WebCrawler crawler = new WebCrawler(
                scraper,
                new SimpleMeterRegistry(),
                htmlFeatureExtractor,
                mock(TechnologyAnalyzer.class));

        WebCrawlResult result = crawler.crawl(visitRequest);

        assertThat(result.getVisitId()).isEqualTo("visit-id");
        assertThat(result.getPageVisits()).hasSize(1);
        assertThat(result.getPageVisits().getFirst().getHtmlFeatures()).isNull();
    }
}
