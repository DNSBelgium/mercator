package be.dnsbelgium.mercator.web.wappalyzer.jappalyzer;

import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

public class TechnologyBuilder {

    private final List<Category> categories = new LinkedList<>();
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final MeterRegistry meterRegistry;

    public TechnologyBuilder(MeterRegistry meterRegistry) {
        this(Collections.emptyList(), meterRegistry);
    }

    public TechnologyBuilder(List<Category> categories, MeterRegistry meterRegistry) {
      this.meterRegistry = meterRegistry;
      this.categories.addAll(categories);
    }

    public Technology fromString(String name, String technologyContent) {
        return fromJSON(name, objectMapper.readTree(technologyContent));
    }

    public Technology fromJSON(String name, JsonNode object) {
        Technology technology = new Technology(name, meterRegistry);
        technology.setDescription(readStringOrEmpty("description", object));
        technology.setWebsite(readStringOrEmpty("website", object));
        technology.setIconName(readStringOrEmpty("icon", object));
        technology.setCPE(readStringOrEmpty("cpe", object));
        technology.setSaas(readBooleanOrFalse("saas", object));

        if (object.has("implies")) {
            List<String> implies = readValuesFromObject(object.get("implies"));
            for (String imply : implies) {
                technology.addImplies(imply);
            }
        }

        if (object.has("cats")) {
            ArrayNode array = (ArrayNode) object.get("cats");
            for (JsonNode node : array) {
                int categoryId = node.asInt();
                Category category = getCategoryById(categoryId);
                if (category != null) {
                    technology.addCategory(category);
                }
            }
        }

        if (object.has("pricing")) {
            ArrayNode pricing = (ArrayNode) object.get("pricing");
            for (JsonNode node : pricing) {
                technology.addPricing(node.asString());
            }
        }

        if (object.has("html")) {
            List<String> htmlTemplates = readValuesFromObject(object.get("html"));
            for (String template : htmlTemplates) {
                technology.addHtmlTemplate(template);
            }
        }

        if (object.has("dom")) {
            List<DomPattern> domPatterns = readDOMPatterns(object.get("dom"));
            for (DomPattern pattern : domPatterns) {
                technology.addDomPattern(pattern);
            }
        }

        if (object.has("scriptSrc")) {
            List<String> scriptSrcTemplates = readValuesFromObject(object.get("scriptSrc"));
            for (String template : scriptSrcTemplates) {
                technology.addScriptSrc(template);
            }
        }

        if (object.has("headers")) {
            ObjectNode headersObject = (ObjectNode) object.get("headers");
            headersObject.properties().forEach(entry -> {
                String header = entry.getKey();
                String headerPattern = entry.getValue().asString();
                technology.addHeaderTemplate(header, headerPattern);
            });
        }

        if (object.has("cookies")) {
            ObjectNode cookiesObject = (ObjectNode) object.get("cookies");
            cookiesObject.properties().forEach(entry -> {
                String cookie = entry.getKey();
                String cookiePattern = entry.getValue().asString();
                technology.addCookieTemplate(cookie, cookiePattern);
            });
        }

        if (object.has("meta")) {
            ObjectNode metaObject = (ObjectNode) object.get("meta");
            metaObject.properties().forEach(entry -> {
                String key = entry.getKey();
                List<String> patterns = readValuesFromObject(entry.getValue());
                for (String pattern : patterns) {
                    technology.addMetaTemplate(key, pattern);
                }
            });
        }
        return technology;
    }

    private List<DomPattern> readDOMPatterns(JsonNode object) {
        List<DomPattern> templates = new LinkedList<>();
        if (object.isString()) {
            templates.add(new DomPattern(meterRegistry, object.asString()));
        } else if (object.isArray()) {
            ArrayNode array = (ArrayNode) object;
            for (JsonNode item : array) {
                if (item.isString()) {
                    templates.add(new DomPattern(meterRegistry, item.asString()));
                }
            }
        } else if (object.isObject()) {
            ObjectNode jsonObject = (ObjectNode) object;
            jsonObject.properties().forEach(entry -> {
                String selector = entry.getKey();
                ObjectNode selectorParams = (ObjectNode) entry.getValue();

                String text = "";
                String exists = null;
                Map<String, String> attributesMap = new HashMap<>();
                Map<String, String> propertiesMap = new HashMap<>();

                if (selectorParams.has("attributes")) {
                    ObjectNode attributesObject = (ObjectNode) selectorParams.get("attributes");
                    attributesObject
                            .properties()
                            .forEach(attrEntry ->
                                    attributesMap.put(attrEntry.getKey(), attrEntry.getValue().asString()));
                }

                if (selectorParams.has("properties")) {
                    ObjectNode propertiesObject = (ObjectNode) selectorParams.get("properties");
                    propertiesObject
                            .properties()
                            .forEach(propEntry
                                    -> propertiesMap.put(propEntry.getKey(), propEntry.getValue().asString())
                            );
                }

                if (selectorParams.has("text")) {
                    text = selectorParams.get("text").asString();
                }

                if (selectorParams.has("exists")) {
                    exists = selectorParams.get("exists").asString();
                }

                templates.add(new DomPattern(meterRegistry, selector, attributesMap, propertiesMap, text, exists));
            });
        }
        return templates;
    }

    private Category getCategoryById(int id) {
        Optional<Category> category = this.categories.stream().filter(item -> item.getId() == id).findFirst();
        return category.orElse(null);
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean readBooleanOrFalse(String key, JsonNode object) {
        return object.has(key) && object.get(key).isBoolean() && object.get(key).asBoolean();
    }

    private static String readStringOrEmpty(String key, JsonNode object) {
        return object.has(key) && object.get(key).isString() ? object.get(key).asString() : "";
    }

    private static List<String> readValuesFromObject(JsonNode jsonObject) {
        List<String> patterns = new LinkedList<>();
        if (jsonObject.isArray()) {
            for (JsonNode arrayItem : jsonObject) {
                patterns.add(arrayItem.asString());
            }
        } else if (jsonObject.isString()) {
            patterns.add(jsonObject.asString());
        }
        return patterns;
    }
}