package be.dnsbelgium.mercator.vat.wappalyzer;

import be.dnsbelgium.mercator.vat.domain.Page;
import be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.Jappalyzer;
import be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.Technology;
import be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer.TechnologyMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TechnologyAnalyzer {

    private final Jappalyzer jappalyzer;

    private final Logger logger = LoggerFactory.getLogger(TechnologyAnalyzer.class);

    public TechnologyAnalyzer() {
        this.jappalyzer = Jappalyzer.create();
    }

    public TechnologyAnalyzer(Jappalyzer jappalyzer) {
        this.jappalyzer = jappalyzer;
    }

    public Set<String> analyze(Page page) {
        return jappalyzer.fromPage(page).stream().map(TechnologyMatch::getTechnology).map(Technology::getName).collect(Collectors.toSet());
    }
    public Set<String> analyze(List<Page> pages) {
        return pages.stream().map(this::analyze).flatMap(Set::stream)
            .collect(Collectors.toSet());
    }
}