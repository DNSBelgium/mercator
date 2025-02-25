package be.dnsbelgium.mercator.wappalyzer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

// japalyzer library imports

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Technology;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.Jappalyzer;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PageResponse;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.TechnologyMatch;

@Service
public class TechnologyAnalyzer {

    private final Jappalyzer jappalyzer;

    public TechnologyAnalyzer() {
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