package be.dnsbelgium.mercator.wappalyzer;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.vampbear.jappalyzer.Jappalyzer;
import static org.assertj.core.api.Assertions.*;

public class TechnologyAnalyzerTest {

    private Jappalyzer jappalyzer;
    private TechnologyAnalyzer technologyAnalyzer;

    @Before
    public void setUp() {
        jappalyzer = Jappalyzer.create();
        technologyAnalyzer = new TechnologyAnalyzer(jappalyzer);
    }

    @Test
    public void givenTechnologyAnalyzer_whenAnalyzeGoogleCom_ReturnDetectedTechnologies() {
        Set<String> detectedTechnologies = technologyAnalyzer.analyze("https://wikipedia.org");
        assertNotNull(detectedTechnologies);
        assertThat(detectedTechnologies).containsExactlyInAnyOrder("Apache Traffic Server", "HSTS", "Open Graph");
    }
}