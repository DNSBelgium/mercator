package be.dnsbelgium.mercator.feature.extraction.health;

import be.dnsbelgium.mercator.feature.extraction.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class FailureRateHealthIndicator implements HealthIndicator {
    @Autowired
    private final MeterRegistry meterRegistry;
    private final double FAILURE_THRESHOLD = 0.02;

    public FailureRateHealthIndicator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private double getFailureRate() {
        double failed = this.meterRegistry.counter(MetricName.COUNTER_VISITS_FAILED).count();
        double succeeded =  this.meterRegistry.counter(MetricName.COUNTER_VISITS_PROCESSED).count();
        if (succeeded == 0) {
            return 0.0;
        }
        return failed / succeeded;
    }

    @Override
    public Health health() {
        double failureRate = this.getFailureRate();
        Health.Builder builder = failureRate  < FAILURE_THRESHOLD ? Health.up() : Health.down();

        builder.withDetail("failureRate", failureRate);

        return builder.build();
    }
}
