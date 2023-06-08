package be.dnsbelgium.mercator.smtp;

import io.micrometer.core.instrument.*;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class smtpCrawlerHealth implements HealthIndicator {
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    @Override
    public Health health() {
        boolean contentCrawlerHealthParameters = true;

        Counter smtpVisitsTotal = getCounter("smtp.analyzer.domains.done");
        Counter smtpVisitsFailed = getCounter("smtp.analyzer.failures");

        double visitsFailed = smtpVisitsFailed.count();
        double visitsTotal = smtpVisitsTotal.count();

        double failureRate = visitsFailed / visitsTotal;


        if (Double.isNaN(failureRate) || Double.compare(failureRate, 0.4) > 0) {
            return Health.down().withDetail("Failure Rate", failureRate).build();
        } else {
            return Health.up().withDetail("Failure rate: ", failureRate).build();
        }
    }
}
