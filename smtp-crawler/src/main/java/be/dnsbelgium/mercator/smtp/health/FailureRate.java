package be.dnsbelgium.mercator.smtp.health;

import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FailureRate implements HealthIndicator {
    @Autowired
    private final MeterRegistry meterRegistry;
    private final Counter inputCounter;
    private final Counter failureCounter;

    private static final double FAILURE_RATE_THRESHOLD = 0.02;


    private Counter getCounter(String counterName) {
        return this.meterRegistry.counter(counterName);
    }

    private final String HEALTHCHECK_NAME = "FailureRate";

    FailureRate(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.inputCounter = getCounter(MetricName.SMTP_DOMAINS_DONE);
        this.failureCounter = getCounter(MetricName.COUNTER_FAILED_VISITS);
    }

    private double getFailureRate() {
        if ((int)this.inputCounter.count() == 0) {
            return 0;
        }
        return this.failureCounter.count() / (this.inputCounter.count() + this.failureCounter.count());
    }

    @Override
    public Health health() {
        double failureRate = getFailureRate();

        Health.Builder builder = failureRate < FailureRate.FAILURE_RATE_THRESHOLD ? Health.up() : Health.down();
        builder.withDetail("failureRate", failureRate);
        return builder.build();
    }
}
