package be.dnsbelgium.mercator.web.wappalyzer;

import be.dnsbelgium.mercator.web.domain.Page;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TechnologyAnalyzerTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private TechnologyAnalyzer technologyAnalyzer;

    @BeforeEach
    public void setUp() {
        technologyAnalyzer = new TechnologyAnalyzer(meterRegistry);
    }

    @Test
    public void testAnalyzeWithPage() {
        String content1 = "<html><head><meta name=\"generator\" content=\"WordPress 5.8\" /></head><body></body></html>";
        String content2 = "<html><head><meta name=\"generator\" content=\"Joomla! - Open Source Content Management\" /></head><body></body></html>";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", List.of("text/html"));

        List<Page> pages = List.of(
            Page.builder().statusCode(200).headers(headers).responseBody(content1).build(),
            Page.builder().statusCode(200).headers(headers).responseBody(content2).build());

        Set<String> detectedTechnologies = technologyAnalyzer.analyze(pages);

        assertThat(detectedTechnologies).isNotNull();
        Set<String> expectedTechnologies = Set.of("WordPress", "Joomla", "MySQL", "PHP");
        assertThat(detectedTechnologies).isEqualTo(expectedTechnologies);
    }

}