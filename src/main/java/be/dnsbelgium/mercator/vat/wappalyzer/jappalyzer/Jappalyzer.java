package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.vat.domain.Page;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// we use an existing implementation of (https://github.com/freekoder/jappalyzer) for json parsing
public class Jappalyzer {

    private List<Technology> technologies = new LinkedList<>();

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

    public Set<TechnologyMatch> fromPage(Page page) {
        return getTechnologyMatches(page);
    }

    public void addTechnology(Technology technology) {
        this.technologies.add(technology);
    }

    private Set<TechnologyMatch> getTechnologyMatches(Page page) {
        Set<TechnologyMatch> matchesSet = technologies
                .stream()
                //.parallel()
                .map(technology -> technology.applicableTo(page))
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
