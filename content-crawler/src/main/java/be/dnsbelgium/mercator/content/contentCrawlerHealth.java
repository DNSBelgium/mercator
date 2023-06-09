package be.dnsbelgium.mercator.content;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class contentCrawlerHealth implements HealthIndicator {
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    @Override
    public Health health() {
        Counter contentCrawlerMuppetsMessagesIn = getCounter("content.crawler.muppets.messages.in");
        Counter contentCrawlerWappalyserMessagesIn = getCounter("content.crawler.wappalyzer.messages.in");
        Counter contentCrawlerMessagesOut = getCounter("content.crawler.messages.out");
        Counter contentCrawlerMessagesFailed = getCounter("content.crawler.messages.failed");

        double muppetsMessagesIn = contentCrawlerMuppetsMessagesIn.count();
        double wappalyserMessagesIn = contentCrawlerWappalyserMessagesIn.count();
        double contentMessagesOut = contentCrawlerMessagesOut.count();
        double contentMessagesFailed = contentCrawlerMessagesFailed.count();

        double failureRate = contentMessagesFailed / (contentMessagesOut + contentMessagesFailed);

        if (Double.isNaN(failureRate) || Double.compare(failureRate, 0.4) > 0) {
            return Health.down().withDetail("Failure Rate", failureRate).build();
        } else {
            return Health.up().withDetail("Failure rate: ", failureRate).build();
        }
    }
}
