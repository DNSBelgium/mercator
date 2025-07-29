package be.dnsbelgium.mercator.web.wappalyzer;

import be.dnsbelgium.mercator.web.domain.Page;
import be.dnsbelgium.mercator.web.wappalyzer.jappalyzer.Jappalyzer;
import be.dnsbelgium.mercator.web.wappalyzer.jappalyzer.Technology;
import be.dnsbelgium.mercator.web.wappalyzer.jappalyzer.TechnologyMatch;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static be.dnsbelgium.mercator.web.wappalyzer.jappalyzer.MetricName.*;

@Service
public class TechnologyAnalyzer {

    private final Jappalyzer jappalyzer;
    private final Timer analyzePageTimer;

    public TechnologyAnalyzer(MeterRegistry meterRegistry) {
        this.jappalyzer = Jappalyzer.create(meterRegistry);
        this.analyzePageTimer = Timer
                .builder(TIMER_JAPPALYZER_ANALYZE_PAGE)
                .publishPercentiles(0.5, 0.80, 0.95, 0.99)
                .description("Time needed to find technologies on a page")
                .register(meterRegistry);
    }

    public Set<String> analyze(Page page) {
        if (page.getDocument() == null) {
            return Set.of();
        }
        return analyzePageTimer.record(
                () ->
                jappalyzer
                        .fromPage(page)
                        .stream()
                        .map(TechnologyMatch::getTechnology)
                        .map(Technology::getName)
                        .collect(Collectors.toSet())
        );
    }

    public Set<String> analyze(List<Page> pages) {
        return pages
                .stream()
                .map(this::analyze)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
}