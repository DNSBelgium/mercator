package be.dnsbelgium.mercator.vat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class vatCrawlerHealth implements HealthIndicator {
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    @Override
    public Health health() {
        Counter vatVisitsCompleted = getCounter("vat.crawler.visits");
        Counter vatVisitsFailed = getCounter("vat.crawler.failures");

        double visitsFailed = vatVisitsFailed.count();
        double visitsCompleted = vatVisitsCompleted.count();

        double failureRate = visitsFailed / (visitsCompleted + visitsFailed);

        if (Double.isNaN(failureRate) || Double.compare(failureRate, 0.4) > 0) {
            return Health.down().withDetail("Failure Rate", failureRate).build();
        } else {
            return Health.up().withDetail("Failure rate: ", failureRate).build();
        }
    }
}
