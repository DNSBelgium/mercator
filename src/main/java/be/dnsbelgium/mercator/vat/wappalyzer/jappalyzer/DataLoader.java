package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class DataLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    public List<Technology> loadInternalTechnologies() {
        Map<Integer, Group> idGroupMap = readInternalGroups();
        List<Category> categories = readInternalCategories(idGroupMap);
        return readTechnologiesFromInternalResources(categories);
    }

    private Map<Integer, Group> readInternalGroups() {
        try {
            String groupsContent = readFileContentFromResource("groups.json");
            return createGroupsMap(objectMapper.readTree(groupsContent));
        } catch (IOException  e) {
            logger.error("Failed to load 'groups.json'", e);
        }
        return Collections.emptyMap();
    }


    private Map<Integer, Group> createGroupsMap(JsonNode groupsJSON) {
        Map<Integer, Group> idGroupMap = new HashMap<>();
        groupsJSON.fields().forEachRemaining(entry -> {
            int id = Integer.parseInt(entry.getKey());
            JsonNode groupObject = entry.getValue();
            idGroupMap.put(id, new Group(id, groupObject.get("name").asText()));
        });
        return idGroupMap;

    }



    private Category extractCategory(JsonNode categoryJSON, String key, Map<Integer, Group> idGroupMap) {
        List<Integer> groupsIds = readGroupIds(categoryJSON);
        List<Group> groups = convertIdsToGroups(idGroupMap, groupsIds);
        Category category = new Category(
                Integer.parseInt(key), categoryJSON.get("name").asText(), categoryJSON.get("priority").asInt());
        category.setGroups(groups);
        return category;
    }



    private List<Category> readInternalCategories(Map<Integer, Group> idGroupMap) {
        List<Category> categories = new LinkedList<>();
        try {
            String categoriesContent = readFileContentFromResource("categories.json");
            JsonNode categoriesJSON = objectMapper.readTree(categoriesContent);
            categoriesJSON.fields().forEachRemaining(entry -> {
                JsonNode categoryJson = entry.getValue();
                categories.add(extractCategory(categoryJson, entry.getKey(), idGroupMap));
            });
        } catch (IOException  e) {
            logger.error("Failed to load 'categories.json'", e);
        }
        return categories;
    }

    private List<Group> convertIdsToGroups(Map<Integer, Group> idGroupMap, List<Integer> groupsIds) {
        List<Group> groups = new LinkedList<>();
        for (Integer id : groupsIds) {
            if (idGroupMap.containsKey(id)) {
                groups.add(idGroupMap.get(id));
            }
        }
        return groups;
    }

    private List<Integer> readGroupIds(JsonNode categoryObject) {
        List<Integer> groupsIds = new LinkedList<>();
        if (categoryObject.has("groups")) {
            ArrayNode groupsArray = (ArrayNode) categoryObject.get("groups");
            for (JsonNode groupIdNode : groupsArray) {
                groupsIds.add(groupIdNode.asInt());
            }
        }
        return groupsIds;
    }

    private List<Technology> readTechnologiesFromInternalResources(List<Category> categories) {
        List<Technology> technologies = new LinkedList<>();
        String[] keys = new String[] {
                "a", "b", "c", "d", "e", "f", "g", "h", "i",
                "j", "k", "l", "m", "n", "o", "p", "q", "r",
                "s", "t", "u", "v", "w", "x", "y", "z", "_" };
        for (String key : keys) {
            String techFilename = String.format("technologies/%s.json", key);
            try {
                String fileContent = readFileContentFromResource(techFilename);
                technologies.addAll(readTechnologiesFromString(fileContent, categories));
            } catch (IOException e) {
                logger.error("Failed to load " + techFilename, e);
            }
        }
        return technologies;
    }

    private String readFileContentFromResource(String techFilename) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = Jappalyzer.class.getClassLoader().getResourceAsStream(techFilename)) {
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String str;
                while ((str = br.readLine()) != null) {
                    sb.append(str);
                }
            }
        }
        return sb.toString();
    }

    private List<Technology> readTechnologiesFromString(String technologiesString, List<Category> categories) {
        List<Technology> technologies = new LinkedList<>();
        JsonNode fileJSON;
        try {
            fileJSON = objectMapper.readTree(technologiesString);
        } catch (IOException e) {
            logger.error("Failed to load " + technologiesString, e);
            return technologies;
        }
        TechnologyBuilder technologyBuilder = new TechnologyBuilder(categories);
        fileJSON.fields().forEachRemaining(entry -> {
            JsonNode object = entry.getValue();
            try {
                Technology technology = technologyBuilder.fromJSON(entry.getKey(), object);
                technologies.add(technology);
            } catch (Exception e) {
                logger.atError()
                        .setMessage("Failed to load JSON for {}")
                        .addArgument(technologiesString)
                        .setCause(e)
                        .log();
            }
        });
        return technologies;

   
    }

}
