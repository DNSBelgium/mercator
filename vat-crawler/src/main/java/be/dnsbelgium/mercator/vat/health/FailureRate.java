package be.dnsbelgium.mercator.vat.health;

import be.dnsbelgium.mercator.vat.metrics.MetricName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FailureRate implements HealthIndicator {
    @Autowired
    private final MeterRegistry meterRegistry;
    private final Counter successCounter;
    private final Counter failedCounter;

    // TODO: read from config
    private static final double FAILURE_RATE_THRESHOLD = 0.02;


    private Counter getCounter(String counterName) {
        return this.meterRegistry.counter(counterName);
    }

    private final String HEALTHCHECK_NAME = "FailureRate";

    FailureRate(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.successCounter = getCounter(MetricName.COUNTER_SUCCESS_VISITS);
        this.failedCounter = getCounter(MetricName.COUNTER_FAILED_VISITS);
    }

    private double getFailureRate() {
        if ((int)this.successCounter.count() == 0 && (int)this.failedCounter.count() == 0) {
            return 0;
        }
        return this.failedCounter.count() / (this.successCounter.count() + this.failedCounter.count());
    }

    @Override
    public Health health() {
        double failureRate = getFailureRate();

        Health.Builder builder = failureRate < FailureRate.FAILURE_RATE_THRESHOLD ? Health.up() : Health.down();
        builder.withDetail("failureRate", failureRate);
        return builder.build();
    }
}
