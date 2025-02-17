package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

import java.util.*;

import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import org.json.JSONException;
import java.io.InputStreamReader;

public class DataLoader {

    public List<Technology> loadInternalTechnologies() {
        Map<Integer, Group> idGroupMap = readInternalGroups();
        List<Category> categories = readInternalCategories(idGroupMap);
        return readTechnologiesFromInternalResources(categories);
    }

    private Map<Integer, Group> readInternalGroups() {
        try {
            String groupsContent = readFileContentFromResource("groups.json");
            return createGroupsMap(new JSONObject(groupsContent));
        } catch (IOException | JSONException ignore) {
        }
        return Collections.emptyMap();
    }

    /*
     * public List<Technology> loadLatestTechnologies() {
     * Map<Integer, Group> idGroupMap = readLatestGroups();
     * List<Category> categories = readLatestCategories(idGroupMap);
     * return readTechnologiestFromGit(categories);
     * }
     * 
     * private Map<Integer, Group> readLatestGroups() {
     * HttpClient httpClient = new HttpClient();
     * try {
     * PageResponse pageResponse = httpClient.getPageByUrl(GROUPS_GIT_PATH);
     * JSONObject groupsContent = new JSONObject(pageResponse.getOrigContent());
     * return createGroupsMap(new JSONObject(groupsContent));
     * } catch (IOException | JSONException ignore) {
     * }
     * return Collections.emptyMap();
     * }
     */

    private Map<Integer, Group> createGroupsMap(JSONObject groupsJSON) {
        Map<Integer, Group> idGroupMap = new HashMap<>();
        for (String key : groupsJSON.keySet()) {
            JSONObject groupObject = groupsJSON.getJSONObject(key);
            int id = Integer.parseInt(key);
            idGroupMap.put(id, new Group(id, groupObject.getString("name")));
        }
        return idGroupMap;
    }

    /*
     * private List<Category> readLatestCategories(Map<Integer, Group> idGroupMap) {
     * List<Category> categories = new LinkedList<>();
     * HttpClient httpClient = new HttpClient();
     * try {
     * PageResponse pageResponse = httpClient.getPageByUrl(CATEGORIES_GIT_PATH);
     * JSONObject categoriesJSON = new JSONObject(pageResponse.getOrigContent());
     * for (String key : categoriesJSON.keySet()) {
     * JSONObject categoryJSON = categoriesJSON.getJSONObject(key);
     * categories.add(extractCategory(categoryJSON, key, idGroupMap));
     * }
     * } catch (IOException | JSONException ignore) {
     * }
     * return categories;
     * }
     */

    private Category extractCategory(JSONObject categoryJSON, String key, Map<Integer, Group> idGroupMap) {
        List<Integer> groupsIds = readGroupIds(categoryJSON);
        List<Group> groups = convertIdsToGroups(idGroupMap, groupsIds);
        Category category = new Category(
                Integer.parseInt(key), categoryJSON.getString("name"), categoryJSON.getInt("priority"));
        category.setGroups(groups);
        return category;
    }

    /*
     * private List<Technology> readTechnologiestFromGit(List<Category> categories)
     * {
     * List<Technology> technologies = new LinkedList<>();
     * HttpClient httpClient = new HttpClient();
     * String[] keys = new String[] {
     * "a", "b", "c", "d", "e", "f", "g", "h", "i",
     * "j", "k", "l", "m", "n", "o", "p", "q", "r",
     * "s", "t", "u", "v", "w", "x", "y", "z", "_" };
     * try {
     * for (String key : keys) {
     * String techGithubUrl = String.format(TECHNOLOGIES_GIT_PATH_TEMPLATE, key);
     * PageResponse pageResponse = httpClient.getPageByUrl(techGithubUrl);
     * technologies.addAll(
     * readTechnologiesFromString(pageResponse.getOrigContent(), categories));
     * }
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * return technologies;
     * }
     */

    private List<Category> readInternalCategories(Map<Integer, Group> idGroupMap) {
        List<Category> categories = new LinkedList<>();
        try {
            String categoriesContent = readFileContentFromResource("categories.json");
            JSONObject categoriesJSON = new JSONObject(categoriesContent);
            for (String key : categoriesJSON.keySet()) {
                JSONObject categoryJSON = categoriesJSON.getJSONObject(key);
                categories.add(extractCategory(categoryJSON, key, idGroupMap));
            }
        } catch (IOException | JSONException ignore) {
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

    private List<Integer> readGroupIds(JSONObject categoryObject) {
        List<Integer> groupsIds = new LinkedList<>();
        if (categoryObject.has("groups")) {
            for (int i = 0; i < categoryObject.getJSONArray("groups").length(); i++) {
                int id = categoryObject.getJSONArray("groups").getInt(i);
                groupsIds.add(id);
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
            } catch (IOException ignore) {
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
        JSONObject fileJSON = new JSONObject(technologiesString);
        TechnologyBuilder technologyBuilder = new TechnologyBuilder(categories);
        for (String key : fileJSON.keySet()) {
            JSONObject object = (JSONObject) fileJSON.get(key);
            try {
                Technology technology = technologyBuilder.fromJSON(key, object);
                technologies.add(technology);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return technologies;
    }

}
