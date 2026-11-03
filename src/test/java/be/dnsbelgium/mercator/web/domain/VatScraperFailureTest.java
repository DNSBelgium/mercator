package be.dnsbelgium.mercator.web.domain;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VatScraperFailureTest {

    @Test
    void leavesVatValuesEmptyWhenVatExtractionFails() {
        VatFinder vatFinder = mock(VatFinder.class);
        when(vatFinder.findValidVatValues("Example")).thenThrow(new IllegalArgumentException("invalid VAT input"));
        VatScraper scraper = new VatScraper(
                new SimpleMeterRegistry(),
                mock(PageFetcher.class),
                vatFinder,
                new VatLinkPrioritizer());
        Page page = Page.builder()
                .url(HttpUrl.get("https://www.example.be/"))
                .visitStarted(Instant.now())
                .visitFinished(Instant.now())
                .statusCode(200)
                .responseBody("<html><body>Example</body></html>")
                .headers(Map.of())
                .build();

        scraper.updateGoal(page);

        assertThat(page.getVatValues()).isEmpty();
    }

    @Test
    void leavesVatValuesEmptyWhenVatExtractionOverflows() {
        VatFinder vatFinder = mock(VatFinder.class);
        when(vatFinder.findValidVatValues("Example")).thenThrow(new StackOverflowError("simulated regex overflow"));
        VatScraper scraper = new VatScraper(
                new SimpleMeterRegistry(),
                mock(PageFetcher.class),
                vatFinder,
                new VatLinkPrioritizer());
        Page page = Page.builder()
                .url(HttpUrl.get("https://www.example.be/"))
                .visitStarted(Instant.now())
                .visitFinished(Instant.now())
                .statusCode(200)
                .responseBody("<html><body>Example</body></html>")
                .headers(Map.of())
                .build();

        scraper.updateGoal(page);

        assertThat(page.getVatValues()).isEmpty();
    }
}
