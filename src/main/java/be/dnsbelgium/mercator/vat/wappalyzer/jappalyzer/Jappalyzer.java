package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.vat.domain.Page;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.MetricName.TIMER_JAPPALYZER_FROM_PAGE;

// we use an existing implementation of (https://github.com/freekoder/jappalyzer) for json parsing
public class Jappalyzer {

  private final List<Technology> technologies;
  private final MeterRegistry meterRegistry;
  private static final Logger logger = LoggerFactory.getLogger(Jappalyzer.class);

  public static Jappalyzer create(MeterRegistry meterRegistry) {
    DataLoader dataLoader = new DataLoader(meterRegistry);
    List<Technology> technologies = dataLoader.loadInternalTechnologies();
    return new Jappalyzer(technologies, meterRegistry);
  }

  public List<Technology> getTechnologies() {
    return Collections.unmodifiableList(technologies);
  }

  public Jappalyzer(List<Technology> technologies, MeterRegistry meterRegistry) {
    this.technologies = technologies;
    this.meterRegistry = meterRegistry;
  }

  public Set<TechnologyMatch> fromPage(Page page) {
    return meterRegistry
            .timer(TIMER_JAPPALYZER_FROM_PAGE)
            .record(() -> getTechnologyMatches(page));
  }

  private Set<TechnologyMatch> getTechnologyMatches(Page page) {
    JappalyzerPage jappalyzerPage = new JappalyzerPage(page, Instant.now());
    Set<TechnologyMatch> matchesSet = technologies
            .stream()
            .parallel()
            .map(technology -> technology.applicableTo(jappalyzerPage))
            .filter(TechnologyMatch::isMatched)
            .collect(Collectors.toSet());
    if (jappalyzerPage.getDuration().toMillis() > 5000) {
      logger.info("Spent {} on page with url '{}'", jappalyzerPage.getDuration(), page.getUrl());
    }
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
