// copied from jappalyzer library
package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.test.ResourceReader;
import be.dnsbelgium.mercator.vat.domain.Page;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        Page page = new Page((HttpUrl) null, null, null, 200, pageContent, pageContent.length(), null, null);
        assertThat(technology.applicableTo(page)).isEqualTo(expected);
    }

    @Test
    public void emptyHeaderTest() {
        Technology technology = new Technology("test");
        technology.addHeaderTemplate("X-Flex-Lang", "");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Flex-Lang", Collections.singletonList("IT"));
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).headers(headers).build())).isEqualTo(expected);
    }

    @Test
    public void emptyHeaderPageLowerCaseTest() {
        Technology technology = new Technology("test");
        technology.addHeaderTemplate("X-Flex-Lang", "");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-flex-lang", Collections.singletonList("IT"));
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).headers(headers). responseBody("").build())).isEqualTo(expected);
    }

    @Test
    public void emptyHeaderTechnologyLowerCaseTest() {
        Technology technology = new Technology("test");
        technology.addHeaderTemplate("x-flex-lang", "");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Flex-Lang", Collections.singletonList("IT"));
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).headers(headers).responseBody("").build())).isEqualTo(expected);
    }

    @Test
    public void emptyCookieTechnologyTest() {
        Technology technology = new Technology("test");
        technology.addCookieTemplate("forterToken", "");
        Page page = Page.builder().statusCode(200).headers(Map.of("cookie", List.of("forterToken="))).build();
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.COOKIE);
        assertThat(technology.applicableTo(page)).isEqualTo(expected);
    }

    @Test
    public void serverHeaderTest() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Server", Collections.singletonList("nginx"));
        Technology technology = new Technology("Nginx");
        technology.addHeaderTemplate("Server", "nginx(?:/([\\d.]+))?\\;version:\\1");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).headers(headers).build())).isEqualTo(expected);
    }

    @Test
    @Disabled
    public void cookieHeaderTest() {
        Technology technology = new Technology("Trbo");
        technology.addCookieTemplate("trbo_session", "^(?:[\\d]+)$");
        Page page = Page.builder().statusCode(200).headers(Map.of("cookie", List.of("trbo_session=12312312"))).build();
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.COOKIE);
        assertThat(technology.applicableTo(page)).isEqualTo(expected);
    }

    @Test
    public void scriptTest() throws IOException {
        Technology technology = new Technology("test");
        technology.addScriptSrc("livewire(?:\\.min)?\\.js");
        String htmlContent = ResourceReader.readFileToString("contents/page_with_script.html");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.SCRIPT);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).responseBody(htmlContent).build())).isEqualTo(expected);
    }

    @Test
    public void shouldMatchWithMeta() throws IOException {
        String techDescription = ResourceReader.readFileToString("technologies/joomla.json");
        Technology technology = this.technologyBuilder.fromString("Joomla", techDescription);
        String htmlContent = ResourceReader.readFileToString("contents/joomla_meta.html");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.META, 0L);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).responseBody(htmlContent).build())).isEqualTo(expected);
    }

    @Test
    public void shouldMatchMetaWithEmptyPattern() throws IOException {
        String techDesc = ResourceReader.readFileToString("technologies/jquery_pjax.json");
        Technology technology = this.technologyBuilder.fromString("JQuery pjax", techDesc);
        String htmlContent = ResourceReader.readFileToString("contents/page_with_meta.html");
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.META, 0L);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).responseBody(htmlContent).build())).isEqualTo(expected);
    }

    @Test
    public void shouldMatchWithHeader() throws IOException {
        String techDesc = ResourceReader.readFileToString("technologies/wpengine.json");
        Technology technology = this.technologyBuilder.fromString("WP Engine", techDesc);
        TechnologyMatch expected = new TechnologyMatch(technology, TechnologyMatch.HEADER, 0L);
        assertThat(technology.applicableTo(Page.builder().statusCode(200).headers(Map.of("X-Powered-By", List.of("WP Engine"))).responseBody("").build())).isEqualTo(expected);
    }
}