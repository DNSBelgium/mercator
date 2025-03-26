// copied from jappalyzer library
package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.test.ResourceReader;
import be.dnsbelgium.mercator.vat.domain.Page;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JappalyzerTests {

    @Test
    public void shouldDetectAbicartTechnology() throws IOException {
        TechnologyBuilder technologyBuilder = new TechnologyBuilder();
        String techDesc = ResourceReader.readFileToString("technologies/abicart.json");
        Technology technology = technologyBuilder.fromString("Abicart", techDesc);
        String htmlContent = ResourceReader.readFileToString("contents/abicart_meta.html");
        TechnologyMatch match = new TechnologyMatch(technology, TechnologyMatch.META);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).responseBody(htmlContent).build())).isEqualTo(match);
    }

    @Test
    public void shouldReturnTechnologiesWithTwoLevelImplies() {
        Jappalyzer jappalyzer = Jappalyzer.create();
        Set<TechnologyMatch> matches = jappalyzer.fromPage(Page.builder().statusCode(200).responseBody("").headers(Map.of("X-Powered-By", List.of("WP Engine"))).build());
        List<String> techNames = getTechnologiesNames(matches);

        assertThat(techNames).contains("WordPress", "PHP", "MySQL");
        assertThat(getMatchByName("WordPress", matches).getReason()).isEqualTo(TechnologyMatch.IMPLIED);
        assertThat(getMatchByName("PHP", matches).getReason()).isEqualTo(TechnologyMatch.IMPLIED);
        assertThat(getMatchByName("MySQL", matches).getReason()).isEqualTo(TechnologyMatch.IMPLIED);
    }

    private TechnologyMatch getMatchByName(String name, Collection<TechnologyMatch> matches) {
        return matches.stream()
                .filter(item -> item.getTechnology().getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private List<String> getTechnologiesNames(Collection<TechnologyMatch> matches) {
        return matches.stream()
                .map(TechnologyMatch::getTechnology)
                .map(Technology::getName)
                .collect(Collectors.toList());
    }
}