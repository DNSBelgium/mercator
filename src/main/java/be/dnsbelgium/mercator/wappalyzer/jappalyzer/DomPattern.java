
package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomPattern {

    private final String selector;
    private final Map<String, String> attributes;
    private final Map<String, String> properties;
    private final String text;
    private final String exists;

    public DomPattern(String selector) {
        this(selector, Collections.emptyMap());
    }

    public DomPattern(String selector, Map<String, String> attributes) {
        this(selector, attributes, Collections.emptyMap(), "");
    }

    public DomPattern(String selector, Map<String, String> attributes, Map<String, String> properties, String text) {
        this(selector, attributes, properties, text, null);
    }

    public DomPattern(String selector, Map<String, String> attributes, Map<String, String> properties, String text,
            String exists) {
        this.selector = prepareRegexp(selector);
        this.attributes = attributes;
        this.properties = properties;
        this.text = text;
        this.exists = exists;
    }

    public String getSelector() {
        return selector;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public boolean applicableToDocument(Document document) {
        try {
            Elements elements = document.select(selector);
            if (elements.size() > 0) {
                if ((exists != null) || hasNoElementConstraints(attributes, properties, text))
                    return true;

                for (Element element : elements) {
                    if (matchedWithConstraints(element, attributes, properties, text))
                        return true;
                }
                return false;
            }

        } catch (Selector.SelectorParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean matchedWithConstraints(Element element, Map<String, String> attributes,
            Map<String, String> properties, String text) {
        if (!text.isEmpty()) {
            Pattern pattern = Pattern.compile(prepareRegexp(text));
            Matcher matcher = pattern.matcher(element.text());
            if (matcher.find()) {
                return true;
            }
        }

        if (properties.isEmpty()) {
            return elementMatchAttributes(element, attributes);
        }
        return false;
    }

    private boolean hasNoElementConstraints(Map<String, String> attributes, Map<String, String> properties,
            String text) {
        return attributes.isEmpty() && properties.isEmpty() && text.isEmpty();
    }

    private boolean elementMatchAttributes(Element element, Map<String, String> attributes) {
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
            Pattern pattern = Pattern.compile(prepareRegexp(attributes.get(attribute)));
            Matcher matcher = pattern.matcher(attrValue);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
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

    private String prepareRegexp(String pattern) {
        String[] splittedPattern = pattern.split("\\\\;");
        return splittedPattern[0];
    }
}
