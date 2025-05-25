
package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.MetricName.TIMER_JAPPALYZER_DOM_PATTERN_APPLICABLE;
import static be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.MetricName.TIMER_JAPPALYZER_DOM_PATTERN_COMPILE;

public class DomPattern {

    private final String selector;
    private final Map<String, String> attributes;
    private final Map<String, String> properties;
    private final String text;
    private final String exists;

    private final Evaluator evaluator;

    private final MeterRegistry meterRegistry;
    private final Timer compileTimer;
    private final Timer applicableTimer;

    private final Pattern textPattern;
    private final Map<String, Pattern> attributePatterns;

    private static final Logger logger = LoggerFactory.getLogger(DomPattern.class);

    public DomPattern(MeterRegistry meterRegistry, String selector) {
        this(meterRegistry, selector, Collections.emptyMap(), Collections.emptyMap(), "", null);
    }

    public DomPattern(MeterRegistry meterRegistry, String selector, Map<String, String> attributes, Map<String, String> properties, String text, String exists) {
        this.selector = prepareRegexp(selector);
        this.attributes = attributes;
        this.properties = properties;
        this.text = text;
        this.exists = exists;
        this.evaluator = new CappedEvaluator(QueryParser.parse(selector), 2000);
        this.meterRegistry = meterRegistry;
        this.compileTimer = Timer
                .builder(TIMER_JAPPALYZER_DOM_PATTERN_COMPILE)
                .publishPercentiles(0.50, 0.75, 0.95, 0.99)
                .register(meterRegistry);
        this.applicableTimer = Timer
                .builder(TIMER_JAPPALYZER_DOM_PATTERN_APPLICABLE)
                .publishPercentiles(0.50, 0.75, 0.95, 0.99)
                .register(meterRegistry);
        textPattern = compile(this.text);

        this.attributePatterns = new HashMap<>();
        for (String attribute : attributes.keySet()) {
            String patternString = attributes.get(attribute);
            attributePatterns.put(attribute, compile(patternString));
        }

    }

    private List<Element> select(Document document) {
        long start = System.currentTimeMillis();
        try {
            return document.select(evaluator);
        } finally {
            long milliseconds = System.currentTimeMillis() - start;
            meterRegistry.timer("document.select").record(milliseconds, TimeUnit.MILLISECONDS);
            if (milliseconds > 500) {
                logger.info("selector '{}' on document with {} nodes took {} millis", selector, document.childNodeSize(), milliseconds);
            }
        }
    }

    public boolean applicableToDocument(Document document) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Element> elements = select(document);
            if (elements.size() > 0) {
                if ((exists != null) || hasNoElementConstraints()) {
                    return true;
                }
                for (Element element : elements) {
                    if (matchedWithConstraints(element)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Selector.SelectorParseException e) {
            logger.warn("SelectorParseException: {}", e.getMessage());
        } finally {
            sample.stop(applicableTimer);
        }
        return false;
    }

    private boolean matchedWithConstraints(Element element) {
        if (!text.isEmpty()) {
            Matcher matcher = textPattern.matcher(element.text());
            if (matcher.find()) {
                return true;
            }
        }
        if (properties.isEmpty()) {
            return elementMatchAttributes(element);
        }
        return false;
    }

    private boolean hasNoElementConstraints() {
        return attributes.isEmpty() && properties.isEmpty() && text.isEmpty();
    }

    private boolean elementMatchAttributes(Element element) {
        for (String attribute : attributes.keySet()) {
            String patternString = attributes.get(attribute);
            if (patternString.isEmpty() && element.hasAttr(attribute)) {
                return true;
            }

            String attrValue = element.attr(attribute);
            if (attrValue.isEmpty()) {
                continue;
            }

            if (patternString.isEmpty()) {
                return true;
            }
            Pattern pattern = attributePatterns.get(attribute);
            if (pattern != null) {
                Matcher matcher = pattern.matcher(attrValue);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        // used in tests
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DomPattern pattern = (DomPattern) o;
        return Objects.equals(selector, pattern.selector) && Objects.equals(attributes, pattern.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, attributes);
    }

    @Override
    public String toString() {
        return "DomPattern{" +
                "selector='" + selector + '\'' +
                ", attributes=" + attributes +
                '}';
    }

    private Pattern compile(String text) {
        return compileTimer.record(
                () -> Pattern.compile(prepareRegexp(text))
        );
    }

    private String prepareRegexp(String pattern) {
        String[] splittedPattern = pattern.split("\\\\;");
        return splittedPattern[0];
    }
}
