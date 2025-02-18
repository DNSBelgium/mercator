// copied from jappalyzer library
package be.dnsbelgium.mercator.wappalyzer.jappalyzerTests;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.DomPattern;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DomPatternTest {

    @Test
    public void shouldBeApplicableToSelectorWithAttributes() {
        Map<String, String> attributes = Collections.singletonMap("src", "www\\.resengo\\.\\w+");
        DomPattern pattern = new DomPattern("iframe[src*='resengo']", attributes);
        Document document = Jsoup.parse(
                "<html><body><iframe src='https://www.resengo.com/iframe'/></body></html>");
        assertThat(pattern.applicableToDocument(document)).isTrue();
    }

    @Test
    public void shouldNotMatchSelectorWithText() {
        DomPattern pattern = new DomPattern(
                "style, script",
                Collections.emptyMap(),
                Collections.emptyMap(),
                "(?:\\.[a-z]+|/media)(?:/[\\w-]+)?/(?:original_images/[\\w-]+|images/[\\w-.]+\\.(?:(?:fill|max|min)-\\d+x\\d+(?:-c\\d+)?|(?:width|height|scale)-\\d+|original))\\.");
        Document document = Jsoup.parse("<html><body><script>const index = 0;</script></body></html>");
        assertThat(pattern.applicableToDocument(document)).isFalse();
    }

    @Test
    public void shouldNotMatchOnEmptyAttribute() {
        Map<String, String> attributes = Collections.singletonMap("sveltekit:prefetch", "");
        DomPattern pattern = new DomPattern("a", attributes);
        Document document = Jsoup.parse("<html><body><a href=\"#\">link</a></body></html>");
        assertThat(pattern.applicableToDocument(document)).isFalse();
    }

    @Test
    public void shouldMatchOnEmptyAttribute() {
        Map<String, String> attributes = Collections.singletonMap("sveltekit:prefetch", "");
        DomPattern pattern = new DomPattern("a", attributes);
        Document document = Jsoup.parse(
                "<html><body><a sveltekit:prefetch href=\"/tutorial\" class=\"cta\">Learn Svelte</a></body></html>");
        assertThat(pattern.applicableToDocument(document)).isTrue();
    }

    @Test
    public void shouldIgnoreDomPatternsWithProperties() {
        Map<String, String> attributes = Collections.emptyMap();
        Map<String, String> properties = Collections.singletonMap("_reactRootContainer", "");
        DomPattern pattern = new DomPattern("body > div", attributes, properties, "");
        Document document = Jsoup.parse("<html><body><div>test</div></body></html>");
        assertThat(pattern.applicableToDocument(document)).isFalse();
    }

    @Test
    public void shouldMatchSelectorWithExists() {
        DomPattern pattern = new DomPattern(
                "iframe[src*='paypal.com'], img[src*='paypal.com'], img[src*='paypalobjects.com'], [aria-labelledby='pi-paypal'], [data-paypal-v4='true']",
                Collections.emptyMap(),
                Collections.emptyMap(),
                "", "");
        Document document = Jsoup.parse("<html><body><iframe src=\"paypal.com\"></iframe></body></html>");
        assertThat(pattern.applicableToDocument(document)).isTrue();
    }

}