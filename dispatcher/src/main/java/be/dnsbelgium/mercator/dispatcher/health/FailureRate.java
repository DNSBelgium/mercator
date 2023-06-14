package be.dnsbelgium.mercator.dispatcher.health;

import be.dnsbelgium.mercator.dispatcher.metrics.MetricName;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
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
    @Autowired
    DispatcherEventRepository dispatcherEventRepository;

    private final String HEALTHCHECK_NAME = "failureRate";
    private Counter getCounter(String counterName) {
        return this.meterRegistry.counter(counterName);
    }

    private final Counter inputCounter;
    //private final Counter dispatcherMessagesOut;
    private final Counter failedCounter;

    // TODO: read from config
    private static final double FAILURE_RATE_THRESHOLD = 0.02;

    FailureRate(MeterRegistry meterRegistry, DispatcherEventRepository dispatcherEventRepository) {
        this.meterRegistry = meterRegistry;
        this.dispatcherEventRepository = dispatcherEventRepository;

        this.inputCounter = this.getCounter(MetricName.MESSAGES_IN);
        this.failedCounter = this.getCounter(MetricName.MESSAGES_FAILED);
    }

    private double getFailureRate() {
        if ((int)this.inputCounter.count() == 0) {
            return 0;
        }
        return this.failedCounter.count() / this.inputCounter.count();
    }

    @Override
    public Health health() {
        double failureRate = getFailureRate();

        Health.Builder builder = failureRate < this.FAILURE_RATE_THRESHOLD ? Health.up() : Health.down();
        builder.withDetail("failureRate", failureRate);
        return builder.build();
    }
}
