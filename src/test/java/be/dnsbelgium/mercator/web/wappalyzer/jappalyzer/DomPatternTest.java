// copied from jappalyzer library
package be.dnsbelgium.mercator.web.wappalyzer.jappalyzer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class DomPatternTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private DomPattern makeDomPattern(String selector, Map<String, String> attributes) {
        return new DomPattern(meterRegistry, selector, attributes, Collections.emptyMap(), "", null);
    }

    @Test
    public void shouldBeApplicableToSelectorWithAttributes() {
        Map<String, String> attributes = Collections.singletonMap("src", "www\\.resengo\\.\\w+");
        DomPattern pattern = makeDomPattern("iframe[src*='resengo']", attributes);
        Document document = Jsoup.parse(
                "<html><body><iframe src='https://www.resengo.com/iframe'/></body></html>");
        assertThat(pattern.applicableToDocument(document)).isTrue();
    }

    @Test
    public void shouldNotMatchSelectorWithText() {
        DomPattern pattern = new DomPattern(
                meterRegistry,
                "style, script",
                Collections.emptyMap(),
                Collections.emptyMap(),
                "(?:\\.[a-z]+|/media)(?:/[\\w-]+)?/(?:original_images/[\\w-]+|images/[\\w-.]+\\.(?:(?:fill|max|min)-\\d+x\\d+(?:-c\\d+)?|(?:width|height|scale)-\\d+|original))\\.",
                null);
        Document document = Jsoup.parse("<html><body><script>const index = 0;</script></body></html>");
        assertThat(pattern.applicableToDocument(document)).isFalse();
    }

    @Test
    public void shouldNotMatchOnEmptyAttribute() {
        Map<String, String> attributes = Collections.singletonMap("sveltekit:prefetch", "");
        DomPattern pattern = makeDomPattern("a", attributes);
        Document document = Jsoup.parse("<html><body><a href=\"#\">link</a></body></html>");
        assertThat(pattern.applicableToDocument(document)).isFalse();
    }

    @Test
    public void shouldMatchOnEmptyAttribute() {
        Map<String, String> attributes = Collections.singletonMap("sveltekit:prefetch", "");
        DomPattern pattern = makeDomPattern("a", attributes);
        Document document = Jsoup.parse(
                "<html><body><a sveltekit:prefetch href=\"/tutorial\" class=\"cta\">Learn Svelte</a></body></html>");
        assertThat(pattern.applicableToDocument(document)).isTrue();
    }

    @Test
    public void shouldIgnoreDomPatternsWithProperties() {
        Map<String, String> attributes = Collections.emptyMap();
        Map<String, String> properties = Collections.singletonMap("_reactRootContainer", "");
        DomPattern pattern = new DomPattern(meterRegistry,"body > div", attributes, properties, "", null);
        Document document = Jsoup.parse("<html><body><div>test</div></body></html>");
        assertThat(pattern.applicableToDocument(document)).isFalse();
    }

    @Test
    public void shouldMatchSelectorWithExists() {
        DomPattern pattern = new DomPattern(
                meterRegistry,
                "iframe[src*='paypal.com'], img[src*='paypal.com'], img[src*='paypalobjects.com'], [aria-labelledby='pi-paypal'], [data-paypal-v4='true']",
                Collections.emptyMap(),
                Collections.emptyMap(),
                "", "");
        Document document = Jsoup.parse("<html><body><iframe src=\"paypal.com\"></iframe></body></html>");
        assertThat(pattern.applicableToDocument(document)).isTrue();
    }

}