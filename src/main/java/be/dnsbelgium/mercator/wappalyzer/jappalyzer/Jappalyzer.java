package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Jappalyzer {

    private List<Technology> technologies = new LinkedList<>();

    public static Jappalyzer empty() {
        return new Jappalyzer();
    }

    public List<Technology> getTechnologies() {
        return this.technologies;
    }

    public static Jappalyzer create() {
        DataLoader dataLoader = new DataLoader();
        List<Technology> technologies = dataLoader.loadInternalTechnologies();
        Jappalyzer jappalyzer = new Jappalyzer();
        jappalyzer.setTechnologies(technologies);
        return jappalyzer;

    }

    private void setTechnologies(List<Technology> technologies) {
        this.technologies = new LinkedList<>(technologies);
    }

    public Set<TechnologyMatch> fromPageResponse(PageResponse pageResponse) {
        return getTechnologyMatches(pageResponse);
    }

    public void addTechnology(Technology technology) {
        this.technologies.add(technology);
    }

    private Set<TechnologyMatch> getTechnologyMatches(PageResponse pageResponse) {
        Set<TechnologyMatch> matchesSet = technologies.stream().parallel()
                .map(technology -> technology.applicableTo(pageResponse))
                .filter(TechnologyMatch::isMatched).collect(Collectors.toSet());
        enrichMatchesWithImpliedTechnologies(matchesSet);
        return matchesSet;
    }

    private void enrichMatchesWithImpliedTechnologies(Set<TechnologyMatch> matchesSet) {
        int currentMatchesSize;
        do {
            currentMatchesSize = matchesSet.size();
            List<TechnologyMatch> impliedMatches = new LinkedList<>();
            for (TechnologyMatch match : matchesSet) {
                for (String implyName : match.getTechnology().getImplies()) {
                    getTechnologyByName(implyName).ifPresent(technology -> {
                        TechnologyMatch impliedMatch = new TechnologyMatch(technology, TechnologyMatch.IMPLIED);
                        impliedMatches.add(impliedMatch);
                    });
                }
            }
            for (TechnologyMatch match : impliedMatches) {
                if (!alreadyContainsTechnology(match.getTechnology(), matchesSet)) {
                    matchesSet.add(match);
                }
            }
        } while (matchesSet.size() != currentMatchesSize);
    }

    private boolean alreadyContainsTechnology(Technology technology, Set<TechnologyMatch> matchesSet) {
        for (TechnologyMatch match : matchesSet) {
            if (match.getTechnology().getName().equals(technology.getName())) {
                return true;
            }
        }
        return false;
    }

    private Optional<Technology> getTechnologyByName(String name) {
        return this.technologies.stream()
                .filter(item -> item.getName().equals(name))
                .findFirst();
    }

}
