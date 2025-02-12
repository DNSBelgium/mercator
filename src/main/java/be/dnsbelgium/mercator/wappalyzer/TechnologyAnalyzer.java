package be.dnsbelgium.mercator.wappalyzer;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

// japalyzer library imports

import com.vampbear.jappalyzer.Technology;
import com.vampbear.jappalyzer.Jappalyzer;
import com.vampbear.jappalyzer.TechnologyMatch;
@Service
public class TechnologyAnalyzer {

    private final Jappalyzer jappalyzer;

    public TechnologyAnalyzer() {
        // create method uses the internal json lists compiled with the library, using .latest() would retrieve the latest filterlists from the original wappalyzer repo (to be implemented with webappanalyzer)
        // it will not work because wappalyzer repository has been privated
        this.jappalyzer = Jappalyzer.create();
    }

    public Set<String> analyze(String url) {
        Set<TechnologyMatch> technologyMatches;
        try {
            technologyMatches = jappalyzer.fromUrl(url); // analyze by url, can also be done by html content
            // TODO: implement this with existing requests somewhere else in mercator to the site because the compiled library uses a seperate httpclient request to the site
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze URL: " + url, e);
        }
        Set<String> detectedTechnologies = technologyMatches.stream()
                .map(TechnologyMatch::getTechnology)
                .map(Technology::getName)
                .collect(Collectors.toSet());
        return detectedTechnologies;
    }
}