// copied from jappalyzer library
package be.dnsbelgium.mercator.web.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.test.ResourceReader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TechnologyBuilderTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    public void shouldContainsBasicFields() throws IOException {
        Technology technology = buildTechnologyFromFile("DERAK.CLOUD", "derak.json");
        assertThat(technology.getName()).isEqualTo("DERAK.CLOUD");
        assertThat(technology.getDescription()).isEqualTo("Derak cloud service");
        assertThat(technology.getWebsite()).isEqualTo("https://derak.cloud");
        assertThat(technology.getIconName()).isEqualTo("DerakCloud.png");
        assertThat(technology.getHeaderTemplates("Derak-Umbrage").getFirst().getPattern()).isEmpty();
        assertThat(technology.getHeaderTemplates("Server").getFirst().getPattern()).isEqualTo("^DERAK\\.CLOUD$");
    }

    @Test
    public void shouldReadCPEFromTechDescription() throws IOException {
        Technology technology = buildTechnologyFromFile("Joomla", "joomla.json");
        assertThat(technology.getCpe()).isEqualTo("cpe:/a:joomla:joomla");
    }

    @Test
    public void shouldBeIncludedInTwoCategories() throws IOException {
        Technology technology = buildTechnologyFromFile("Pace", "pace.json");
        assertThat(technology.getCategories()).containsExactlyInAnyOrder(
                new Category(41, "Payment processors", 8),
                new Category(91, "Buy now pay later", 9));
    }

    @Test
    public void shouldTechnologyHasTwoMetaGenerators() throws IOException {
        Technology technology = buildTechnologyFromFile("Abicart", "abicart.json");
        List<PatternWithVersion> generatorTemplates = technology.getMetaTemplates("generator");
        List<String> templateNames = generatorTemplates.stream()
                .map(PatternWithVersion::getPattern)
                .collect(Collectors.toList());
        assertThat(templateNames).containsExactlyInAnyOrder("Abicart", "Textalk Webshop");
    }

    @Test
    public void shouldContainsSaas() throws IOException {
        Technology technology = buildTechnologyFromFile("Jumio", "jumio.json");
        assertThat(technology.isSaas()).isTrue();
    }

    @Test
    public void shouldContainsPricing() throws IOException {
        Technology technology = buildTechnologyFromFile("Jumio", "jumio.json");
        assertThat(technology.getPricing()).containsExactlyElementsOf(Arrays.asList("payg", "mid", "recurring"));
    }

    @Test
    public void shouldReturnEmptyImplies() throws IOException {
        Technology technology = buildTechnologyFromFile("Abicart", "abicart.json");
        assertThat(technology.getImplies()).isEmpty();
    }

    @Test
    public void shouldReturnSingleImpliesValue() throws IOException {
        Technology technology = buildTechnologyFromFile("Warp", "warp.json");
        assertThat(technology.getImplies()).containsExactlyInAnyOrder("Haskell");
    }

    @Test
    public void shouldReturnTwoImpliesValues() throws IOException {
        Technology technology = buildTechnologyFromFile("Wordpress", "wordpress.json");
        assertThat(technology.getImplies()).containsExactlyInAnyOrder("PHP", "MySQL");
    }

    @Test
    public void shouldContainsTwoDOMPatternsFromObject() throws IOException {
        Technology technology = buildTechnologyFromFile("Magento", "magento.json");
        DomPattern expected1 = makeDomPattern(
                "script[data-requiremodule*='mage'], script[data-requiremodule*='Magento_'], html[data-image-optimizing-origin]");
        DomPattern expected2 = makeDomPattern("script[type='text/x-magento-init']");
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void shouldContainsTwoDOMPatternsFromArray() throws IOException {
        Technology technology = buildTechnologyFromFile("Adobe Flash", "adobeflash.json");
        DomPattern expected1 = makeDomPattern("object[type='application/x-shockwave-flash']");
        DomPattern expected2 = makeDomPattern("param[value*='.swf']");
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected1, expected2);
    }

    private DomPattern makeDomPattern(String selector, Map<String, String> attributes) {
        return new DomPattern(meterRegistry, selector, attributes, Collections.emptyMap(), "", "");
    }

    private DomPattern makeDomPattern(String selector) {
        return new DomPattern(meterRegistry, selector, Collections.emptyMap(), Collections.emptyMap(), "", "");
    }

    @Test
    public void shouldContainsAttributesAtDomPattern() throws IOException {
        Technology technology = buildTechnologyFromFile("Rezgo", "rezgo.json");
        Map<String, String> expectedAttrs1 = Collections.singletonMap("id", "rezgo_content_frame");
        DomPattern expected1 = makeDomPattern("iframe", expectedAttrs1);
        Map<String, String> expectedAttrs2 = Collections.singletonMap("href",
                "wp-content/plugins/rezgo/rezgo/templates");
        DomPattern expected2 = makeDomPattern("link", expectedAttrs2);
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void shouldContainsOneDOMPatternFromString() throws IOException {
        Technology technology = buildTechnologyFromFile("Jetpack", "jetpack.json");
        DomPattern expected = makeDomPattern("link[href*='/wp-content/plugins/jetpack/']");
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected);
    }

    private Technology buildTechnologyFromFile(String Abicart, String techFilename) throws IOException {
        String techDesc =  ResourceReader.readFileToString("technologies/" + techFilename);
        List<Category> categories = Arrays.asList(
                new Category(41, "Payment processors", 8),
                new Category(91, "Buy now pay later", 9),
                new Category(22, "TEST CATEGORY 1", 9),
                new Category(33, "TEST CATEGORY 2", 9));
        TechnologyBuilder technologyBuilder = new TechnologyBuilder(categories, meterRegistry);
        return technologyBuilder.fromString(Abicart, techDesc);
    }

}