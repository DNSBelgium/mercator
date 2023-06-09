package be.dnsbelgium.mercator.tls;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Counter;


@Component
public class tlsCrawlerHealth implements HealthIndicator {
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    @Override
    public Health health() {
        Counter tlsVisitsCompleted = getCounter("tls.crawler.visits.completed");
        Counter tlsVisitsFailed = getCounter("tls.crawler.visits.failed");
//      these should be counted but only the values within the last 24h
        double visitsFailed = tlsVisitsFailed.count();
        double visitsCompleted = tlsVisitsCompleted.count();

        double failureRate = visitsFailed / (visitsCompleted + visitsFailed);

        if (Double.isNaN(failureRate) || Double.compare(failureRate, 0.4) > 0) {
            return Health.down().withDetail("Failure Rate", failureRate).build();
        } else {
            return Health.up().withDetail("Failure rate: ", failureRate).build();
        }
    }
}
