package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;

public class TechnologyBuilder {

    private final List<Category> categories = new LinkedList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TechnologyBuilder() {
        this(Collections.emptyList());
    }

    public TechnologyBuilder(List<Category> categories) {
        this.categories.addAll(categories);
    }

    public Technology fromString(String name, String technologyContent) throws IOException {
        return fromJSON(name, objectMapper.readTree(technologyContent));
    }

    public Technology fromJSON(String name, JsonNode object) {
        Technology technology = new Technology(name);
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
            ArrayNode pricings = (ArrayNode) object.get("pricing");
            for (JsonNode node : pricings) {
                technology.addPricing(node.asText());
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
            headersObject.fields().forEachRemaining(entry -> {
                String header = entry.getKey();
                String headerPattern = entry.getValue().asText();
                technology.addHeaderTemplate(header, headerPattern);
            });
        }

        if (object.has("cookies")) {
            ObjectNode cookiesObject = (ObjectNode) object.get("cookies");
            cookiesObject.fields().forEachRemaining(entry -> {
                String cookie = entry.getKey();
                String cookiePattern = entry.getValue().asText();
                technology.addCookieTemplate(cookie, cookiePattern);
            });
        }

        if (object.has("meta")) {
            ObjectNode metaObject = (ObjectNode) object.get("meta");
            metaObject.fields().forEachRemaining(entry -> {
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
        if (object.isTextual()) {
            templates.add(new DomPattern(object.asText()));
        } else if (object.isArray()) {
            ArrayNode array = (ArrayNode) object;
            for (JsonNode item : array) {
                if (item.isTextual()) {
                    templates.add(new DomPattern(item.asText()));
                }
            }
        } else if (object.isObject()) {
            ObjectNode jsonObject = (ObjectNode) object;
            jsonObject.fields().forEachRemaining(entry -> {
                String selector = entry.getKey();
                ObjectNode selectorParams = (ObjectNode) entry.getValue();

                String text = "";
                String exists = null;
                Map<String, String> attributesMap = new HashMap<>();
                Map<String, String> propertiesMap = new HashMap<>();

                if (selectorParams.has("attributes")) {
                    ObjectNode attributesObject = (ObjectNode) selectorParams.get("attributes");
                    attributesObject.fields().forEachRemaining(attrEntry -> {
                        attributesMap.put(attrEntry.getKey(), attrEntry.getValue().asText());
                    });
                }

                if (selectorParams.has("properties")) {
                    ObjectNode propertiesObject = (ObjectNode) selectorParams.get("properties");
                    propertiesObject.fields().forEachRemaining(propEntry -> {
                        propertiesMap.put(propEntry.getKey(), propEntry.getValue().asText());
                    });
                }

                if (selectorParams.has("text")) {
                    text = selectorParams.get("text").asText();
                }

                if (selectorParams.has("exists")) {
                    exists = selectorParams.get("exists").asText();
                }

                templates.add(new DomPattern(selector, attributesMap, propertiesMap, text, exists));
            });
        }
        return templates;
    }

    private Category getCategoryById(int id) {
        Optional<Category> category = this.categories.stream().filter(item -> item.getId() == id).findFirst();
        return category.orElse(null);
    }

    private static boolean readBooleanOrFalse(String key, JsonNode object) {
        return object.has(key) && object.get(key).isBoolean() && object.get(key).asBoolean();
    }

    private static String readStringOrEmpty(String key, JsonNode object) {
        return object.has(key) && object.get(key).isTextual() ? object.get(key).asText() : "";
    }

    private static List<String> readValuesFromObject(JsonNode jsonObject) {
        List<String> patterns = new LinkedList<>();
        if (jsonObject.isArray()) {
            for (JsonNode arrayItem : jsonObject) {
                patterns.add(arrayItem.asText());
            }
        } else if (jsonObject.isTextual()) {
            patterns.add(jsonObject.asText());
        }
        return patterns;
    }
}