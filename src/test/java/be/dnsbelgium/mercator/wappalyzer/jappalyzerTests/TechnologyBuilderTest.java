// copied from jappalyzer library
package be.dnsbelgium.mercator.wappalyzer.jappalyzerTests;

import be.dnsbelgium.mercator.test.ResourceReader;
import org.junit.Test;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Category;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.DomPattern;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PatternWithVersion;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Technology;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.TechnologyBuilder;

import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

public class TechnologyBuilderTest {

    @Test
    public void shouldContainsBasicFields() throws IOException {
        Technology technology = buildTechnologyFromFile("DERAK.CLOUD", "derak.json");
        assertThat(technology.getName()).isEqualTo("DERAK.CLOUD");
        assertThat(technology.getDescription()).isEqualTo("Derak cloud service");
        assertThat(technology.getWebsite()).isEqualTo("https://derak.cloud");
        assertThat(technology.getIconName()).isEqualTo("DerakCloud.png");
        assertThat(technology.getHeaderTemplates("Derak-Umbrage").get(0).getPattern()).isEmpty();
        assertThat(technology.getHeaderTemplates("Server").get(0).getPattern()).isEqualTo("^DERAK\\.CLOUD$");
    }

    @Test
    public void shouldReadCPEFromTechDescription() throws IOException {
        Technology technology = buildTechnologyFromFile("Joomla", "joomla.json");
        assertThat(technology.getCPE()).isEqualTo("cpe:/a:joomla:joomla");
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
        DomPattern expected1 = new DomPattern(
                "script[data-requiremodule*='mage'], script[data-requiremodule*='Magento_'], html[data-image-optimizing-origin]");
        DomPattern expected2 = new DomPattern("script[type='text/x-magento-init']");
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void shouldContainsTwoDOMPatternsFromArray() throws IOException {
        Technology technology = buildTechnologyFromFile("Adobe Flash", "adobeflash.json");
        DomPattern expected1 = new DomPattern("object[type='application/x-shockwave-flash']");
        DomPattern expected2 = new DomPattern("param[value*='.swf']");
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void shouldContainsAttributesAtDomPattern() throws IOException {
        Technology technology = buildTechnologyFromFile("Rezgo", "rezgo.json");
        Map<String, String> expectedAttrs1 = Collections.singletonMap("id", "rezgo_content_frame");
        DomPattern expected1 = new DomPattern("iframe", expectedAttrs1);
        Map<String, String> expectedAttrs2 = Collections.singletonMap("href",
                "wp-content/plugins/rezgo/rezgo/templates");
        DomPattern expected2 = new DomPattern("link", expectedAttrs2);
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void shouldContainsOneDOMPatternFromString() throws IOException {
        Technology technology = buildTechnologyFromFile("Jetpack", "jetpack.json");
        DomPattern expected = new DomPattern("link[href*='/wp-content/plugins/jetpack/']");
        assertThat(technology.getDomPatterns()).containsExactlyInAnyOrder(expected);
    }

    private Technology buildTechnologyFromFile(String Abicart, String techFilename) throws IOException {
        String techDesc =  ResourceReader.readFileToString("technologies/" + techFilename);
        List<Category> categories = Arrays.asList(
                new Category(41, "Payment processors", 8),
                new Category(91, "Buy now pay later", 9),
                new Category(22, "TEST CATEGORY 1", 9),
                new Category(33, "TEST CATEGORY 2", 9));
        TechnologyBuilder technologyBuilder = new TechnologyBuilder(categories);
        return technologyBuilder.fromString(Abicart, techDesc);
    }

}