package be.dnsbelgium.mercator.feature.extraction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class featureExtractionHealth implements HealthIndicator {
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    @Override
    public Health health() {
        boolean contentCrawlerHealthParameters = true;

        Counter featureExtractionVisitsCompleted = getCounter("feature.extraction.visits.processed");
        Counter featureExtractionVisitsFailed = getCounter("feature.extraction.visits.failed");

        double visitsFailed = featureExtractionVisitsFailed.count();
        double visitsCompleted = featureExtractionVisitsCompleted.count();

        double failureRate = visitsFailed / (visitsCompleted + visitsFailed);


        if (Double.isNaN(failureRate) || Double.compare(failureRate, 0.4) > 0) {
            return Health.down().withDetail("Failure Rate", failureRate).build();
        } else {
            return Health.up().withDetail("Failure rate: ", failureRate).build();
        }
    }
}