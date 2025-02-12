package be.dnsbelgium.mercator.wappalyzer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class TechnologyAnalyzer {

    // enkele veelgebruikte libraries, zoekt overal in de html
    private static final Map<String, String[]> TECHNOLOGY_PATTERNS = Map.of(
            "React", new String[]{"react", "react-dom"},
            "Vue.js", new String[]{"vue\\.js"},
            "jQuery", new String[]{"jquery"},
            "Bootstrap", new String[]{"bootstrap"},
            "Nginx", new String[]{"nginx"}
    );

    private final HttpClient httpClient;

    public TechnologyAnalyzer(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // Analyze the HTML content
    private Set<String> analyzeHtml(Document document) {
        Set<String> matches = new HashSet<>();
        String html = document.html().toLowerCase();


        for (Map.Entry<String, String[]> entry : TECHNOLOGY_PATTERNS.entrySet()) {
            String technology = entry.getKey();
            String[] patterns = entry.getValue();

            for (String pattern : patterns) {

                if (html.contains(pattern)) {
                    matches.add(technology);
                }
            }
        }
        return matches; // Return all detected technologies
    }



    public Set<String> analyze(String url) {
        Set<String> detectedTechnologies = new HashSet<>();

        try {

            HttpRequest request = HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // html content
            String htmlContent = response.body();

            // Parse met jsoup
            Document doc = Jsoup.parse(htmlContent);


            detectedTechnologies.addAll(analyzeHtml(doc));
            System.out.println("Detected technologies for " + url + ": " + detectedTechnologies);

        } catch (Exception e) {

            System.err.println("Error fetching or analyzing URL: " + e.getMessage());
        }

        return detectedTechnologies;
    }
}