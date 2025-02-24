// copied from jappalyzer library
package be.dnsbelgium.mercator.wappalyzer.jappalyzerTests;

import be.dnsbelgium.mercator.test.ResourceReader;
import org.junit.Before;
import org.junit.Test;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PageResponse;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Technology;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.TechnologyBuilder;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.TechnologyMatch;

import java.util.*;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;

public class TechnologyTests {

    private TechnologyBuilder technologyBuilder;

    @Before
    public void setUp() {
        this.technologyBuilder = new TechnologyBuilder();
    }

    @Test
    public void shouldMatchHTMLTemplate() throws IOException {
        String pageContent = ResourceReader.readFileToString("contents/font_awesome.html");
        Technology technology = new Technology("Font Awesome");
        technology.addHtmlTemplate(
                "<link[^>]* href=[^>]+(?:([\\d.]+)/)?(?:css/)?font-awesome(?:\\.min)?\\.css\\;version:\\1");
        technology.addHtmlTemplate(
                "<link[^>]* href=[^>]*kit\\-pro\\.fontawesome\\.com/releases/v([0-9.]+)/\\;version:\\1");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HTML);
        assertThat(technology.applicableTo(pageContent)).isEqualTo(expected);
    }

    @Test
    public void emptyHeaderTest() {
        Technology technology = new Technology("test");
        technology.addHeaderTemplate("X-Flex-Lang", "");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Flex-Lang", Collections.singletonList("IT"));
        PageResponse pageResponse = new PageResponse(200, headers, "");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void emptyHeaderPageLowerCaseTest() {
        Technology technology = new Technology("test");
        technology.addHeaderTemplate("X-Flex-Lang", "");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-flex-lang", Collections.singletonList("IT"));
        PageResponse pageResponse = new PageResponse(200, headers, "");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void emptyHeaderTechnologyLowerCaseTest() {
        Technology technology = new Technology("test");
        technology.addHeaderTemplate("x-flex-lang", "");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Flex-Lang", Collections.singletonList("IT"));
        PageResponse pageResponse = new PageResponse(200, headers, "");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void emptyCookieTechnologyTest() {
        Technology technology = new Technology("test");
        technology.addCookieTemplate("forterToken", "");
        PageResponse pageResponse = new PageResponse(200, null, "");
        pageResponse.addCookie("forterToken", "");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.COOKIE);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void serverHeaderTest() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Server", Collections.singletonList("nginx"));
        PageResponse pageResponse = new PageResponse(200, headers, "");
        Technology technology = new Technology("Nginx");
        technology.addHeaderTemplate("Server", "nginx(?:/([\\d.]+))?\\;version:\\1");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void cookieHeaderTest() {
        Technology technology = new Technology("Trbo");
        technology.addCookieTemplate("trbo_session", "^(?:[\\d]+)$");
        PageResponse pageResponse = new PageResponse(200, null, "");
        pageResponse.addCookie("trbo_session", "12312312");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.COOKIE);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void scriptTest() throws IOException {
        Technology technology = new Technology("test");
        technology.addScriptSrc("livewire(?:\\.min)?\\.js");
        String htmlContent = ResourceReader.readFileToString("contents/page_with_script.html");
        PageResponse pageResponse = new PageResponse(200, null, htmlContent);
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.SCRIPT);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void shouldMatchWithMeta() throws IOException {
        String techDescription = ResourceReader.readFileToString("technologies/joomla.json");
        Technology technology = this.technologyBuilder.fromString("Joomla", techDescription);
        String htmlContent = ResourceReader.readFileToString("contents/joomla_meta.html");
        PageResponse pageResponse = new PageResponse(200, null, htmlContent);
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.META, 0L);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void shouldMatchMetaWithEmptyPattern() throws IOException {
        String techDesc = ResourceReader.readFileToString("technologies/jquery_pjax.json");
        Technology technology = this.technologyBuilder.fromString("JQuery pjax", techDesc);
        String htmlContent = ResourceReader.readFileToString("contents/page_with_meta.html");
        PageResponse pageResponse = new PageResponse(200, null, htmlContent);
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.META, 0L);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }

    @Test
    public void shouldMatchWithHeader() throws IOException {
        String techDesc = ResourceReader.readFileToString("technologies/wpengine.json");
        Technology technology = this.technologyBuilder.fromString("WP Engine", techDesc);
        PageResponse pageResponse = new PageResponse(200, null, "");
        pageResponse.addHeader("X-Powered-By", "WP Engine");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER, 0L);
        assertThat(technology.applicableTo(pageResponse)).isEqualTo(expected);
    }
}