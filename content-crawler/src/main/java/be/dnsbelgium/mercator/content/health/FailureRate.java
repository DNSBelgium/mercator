package be.dnsbelgium.mercator.content.health;

import be.dnsbelgium.mercator.content.metrics.MetricName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FailureRate implements HealthIndicator {
    @Autowired
    private final MeterRegistry meterRegistry;

    private final Counter contentCrawlerMuppetsMessagesIn;
    private final Counter contentCrawlerWappalyzerMessagesIn;
    private final Counter contentCrawlerMessagesOut;
    private final Counter contentCrawlerMessagesFailed;

    // TODO: read from config
    private static final double FAILURERATE_THRESHOLD = 0.01;
    // TODO: read from config
    private static final String FAILURE_RATE = "FailureRate";
    private static final String FAILURES = "Failures";

    FailureRate(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.contentCrawlerMuppetsMessagesIn = this.meterRegistry.counter(MetricName.MUPPETS_MESSAGES_IN);
        this.contentCrawlerWappalyzerMessagesIn = this.meterRegistry.counter(MetricName.WAPPALYZER_MESSAGES_IN);
        this.contentCrawlerMessagesOut = this.meterRegistry.counter(MetricName.MESSAGES_OUT);
        this.contentCrawlerMessagesFailed = this.meterRegistry.counter(MetricName.MESSAGES_FAILED);
    }


    private double getFailureRate() {
        double contentMessagesOut = contentCrawlerMessagesOut.count();
        double contentMessagesFailed = contentCrawlerMessagesFailed.count();
        return contentMessagesFailed / contentMessagesOut;
    }


    @Override
    public Health health() {
        // TODO: add timeboxing
        double contentMessagesOut = contentCrawlerMessagesOut.count();
        double contentMessagesFailed = contentCrawlerMessagesFailed.count();

        if ((int)contentMessagesOut == 0)
            return Health.up().withDetail(FAILURE_RATE, 0).withDetail(FAILURES, contentMessagesFailed).build();

        double failureRate = getFailureRate();

        Health.Builder builder = failureRate < FailureRate.FAILURERATE_THRESHOLD ? Health.up() : Health.down();
        builder.withDetail(FAILURE_RATE, failureRate);
        builder.withDetail(FAILURES, contentMessagesFailed);
        return builder.build();
    }
}
