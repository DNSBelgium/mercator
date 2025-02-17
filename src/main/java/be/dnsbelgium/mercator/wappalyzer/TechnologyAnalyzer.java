package be.dnsbelgium.mercator.wappalyzer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.vampbear.jappalyzer.Technology;
import com.vampbear.jappalyzer.Jappalyzer;
import com.vampbear.jappalyzer.PageResponse;
import com.vampbear.jappalyzer.TechnologyMatch;

@Service
public class TechnologyAnalyzer {

    private final Jappalyzer jappalyzer;

    public TechnologyAnalyzer() {
        // create method uses the internal json lists compiled with the library, using
        // .latest() would retrieve the latest filterlists from the original wappalyzer
        // repo (to be implemented with webappanalyzer)
        // it will not work because wappalyzer repository has been privated
        this.jappalyzer = Jappalyzer.create();
    }

    public TechnologyAnalyzer(Jappalyzer jappalyzer) {
        this.jappalyzer = jappalyzer;
    }

    public Set<String> analyze(List<PageResponse> pageResponses) {
        Set<TechnologyMatch> allTechnologyMatches = new HashSet<>();

        for (PageResponse pageResponse : pageResponses) {
            Set<TechnologyMatch> technologyMatches = jappalyzer.fromPageResponse(pageResponse);
            allTechnologyMatches.addAll(technologyMatches);
        }

        Set<String> detectedTechnologies = allTechnologyMatches.stream()
                .map(TechnologyMatch::getTechnology)
                .map(Technology::getName)
                .collect(Collectors.toSet());

        return detectedTechnologies;
    }
}