
package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class TechnologyBuilder {

    private final List<Category> categories = new LinkedList<>();

    public TechnologyBuilder() {
        this(Collections.emptyList());
    }

    public TechnologyBuilder(List<Category> categories) {
        this.categories.addAll(categories);
    }

    public Technology fromString(String name, String technologyContent) {
        return fromJSON(name, new JSONObject(technologyContent));
    }

    public Technology fromJSON(String name, JSONObject object) {
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
            JSONArray array = object.getJSONArray("cats");
            for (int i = 0; i < array.length(); i++) {
                int categoryId = array.getInt(i);
                Category category = getCategoryById(categoryId);
                if (category != null) {
                    technology.addCategory(category);
                }
            }
        }

        if (object.has("pricing")) {
            JSONArray pricings = object.getJSONArray("pricing");
            for (int i = 0; i < pricings.length(); i++) {
                technology.addPricing(pricings.getString(i));
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
            JSONObject headersObject = object.getJSONObject("headers");
            for (String header : headersObject.keySet()) {
                String headerPattern = headersObject.getString(header);
                technology.addHeaderTemplate(header, headerPattern);
            }
        }

        if (object.has("cookies")) {
            JSONObject cookiesObject = object.getJSONObject("cookies");
            for (String cookie : cookiesObject.keySet()) {
                String cookiePattern = cookiesObject.getString(cookie);
                technology.addCookieTemplate(cookie, cookiePattern);
            }
        }

        if (object.has("meta")) {
            JSONObject metaObject = object.getJSONObject("meta");
            for (String key : metaObject.keySet()) {
                List<String> patterns = readValuesFromObject(metaObject.get(key));
                for (String pattern : patterns) {
                    technology.addMetaTemplate(key, pattern);
                }
            }
        }
        return technology;
    }

    private List<DomPattern> readDOMPatterns(Object object) {
        List<DomPattern> templates = new LinkedList<>();
        if (object instanceof String) {
            templates.add(new DomPattern((String) object));
        } else if (object instanceof JSONArray) {
            JSONArray array = (JSONArray) object;
            for (Object item : array) {
                if (item instanceof String) {
                    templates.add(new DomPattern((String) item));
                }
            }
        } else if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            for (String selector : jsonObject.keySet()) {
                JSONObject selectorParams = jsonObject.getJSONObject(selector);

                String text = "";
                String exists = null;
                Map<String, String> attributesMap = new HashMap<>();
                Map<String, String> propertiesMap = new HashMap<>();

                if (selectorParams.has("attributes")) {
                    JSONObject attributesObject = selectorParams.getJSONObject("attributes");
                    for (String attribute : attributesObject.keySet()) {
                        attributesMap.put(attribute, attributesObject.getString(attribute));
                    }
                }

                if (selectorParams.has("properties")) {
                    JSONObject attributesObject = selectorParams.getJSONObject("properties");
                    for (String attribute : attributesObject.keySet()) {
                        propertiesMap.put(attribute, attributesObject.getString(attribute));
                    }
                }

                if (selectorParams.has("text")) {
                    text = selectorParams.getString("text");
                }

                if (selectorParams.has("exists")) {
                    exists = selectorParams.getString("exists");
                }

                templates.add(new DomPattern(selector, attributesMap, propertiesMap, text, exists));
            }
        }
        return templates;
    }

    private Category getCategoryById(int id) {
        Optional<Category> category = this.categories.stream().filter(item -> item.getId() == id).findFirst();
        return category.orElse(null);
    }

    private static boolean readBooleanOrFalse(String key, JSONObject object) {
        if (object.has(key) && (object.get(key) instanceof Boolean)) {
            return object.getBoolean(key);
        } else {
            return false;
        }
    }

    private static String readStringOrEmpty(String key, JSONObject object) {
        if (object.has(key) && (object.get(key) instanceof String)) {
            return object.getString(key);
        } else {
            return "";
        }
    }

    private static List<String> readValuesFromObject(Object jsonObject) {
        List<String> patterns = new LinkedList<>();
        if (jsonObject instanceof JSONArray) {
            for (Object arrayItem : (JSONArray) jsonObject) {
                patterns.add((String) arrayItem);
            }
        } else if (jsonObject instanceof String) {
            patterns.add((String) jsonObject);
        }
        return patterns;
    }
}
