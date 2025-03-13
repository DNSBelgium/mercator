package be.dnsbelgium.mercator.vat.wappalyzer;

import be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.PageResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TechnologyAnalyzerTest {

    private TechnologyAnalyzer technologyAnalyzer;

    @Before
    public void setUp() {
        technologyAnalyzer = new TechnologyAnalyzer();
    }

    @Test
    public void testAnalyze() {
        List<PageResponse> pageResponses = new ArrayList<>();
        String content1 = "<html><head><meta name=\"generator\" content=\"WordPress 5.8\" /></head><body></body></html>";
        String content2 = "<html><head><meta name=\"generator\" content=\"Joomla! - Open Source Content Management\" /></head><body></body></html>";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/html"));

        PageResponse pageResponse1 = new PageResponse(200, headers, content1);
        PageResponse pageResponse2 = new PageResponse(200, headers, content2);

        pageResponses.add(pageResponse1);
        pageResponses.add(pageResponse2);

        Set<String> detectedTechnologies = technologyAnalyzer.analyze(pageResponses);

        assertNotNull(detectedTechnologies);
        Set<String> expectedTechnologies = new HashSet<>(Arrays.asList("WordPress", "Joomla", "MySQL", "PHP"));
        assertEquals(expectedTechnologies, detectedTechnologies);
    }

}