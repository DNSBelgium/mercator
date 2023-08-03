package be.dnsbelgium.mercator.tls.health;

import be.dnsbelgium.mercator.tls.metrics.MetricName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;


@Component
public class FailureRate implements HealthIndicator {
    @Autowired
    private final MeterRegistry meterRegistry;
    private final Counter completedCounter;
    private final Counter failedCounter;

    // TODO: generalize
    private static final double FAILURE_RATE_THRESHOLD = 0.02;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    FailureRate(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.completedCounter = getCounter(MetricName.COUNTER_VISITS_COMPLETED);
        this.failedCounter = getCounter(MetricName.COUNTER_VISITS_FAILED);
    }

    private double getFailureRate() {
        if ((int)this.completedCounter.count() == 0) {
            return 0;
        }
        return this.failedCounter.count() / (this.completedCounter.count() + this.failedCounter.count());
    }

    @Override
    public Health health() {
        double failureRate = getFailureRate();

        Health.Builder builder = failureRate < FailureRate.FAILURE_RATE_THRESHOLD ? Health.up() : Health.down();
        builder.withDetail("failureRate", failureRate);
        return builder.build();
    }
}
