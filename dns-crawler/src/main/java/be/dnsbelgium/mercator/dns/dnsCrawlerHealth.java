package be.dnsbelgium.mercator.dns;

import io.micrometer.core.instrument.*;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class dnsCrawlerHealth implements HealthIndicator {
    private final MeterRegistry meterRegistry = Metrics.globalRegistry;

    private Counter getCounter(String counterName) {
        return meterRegistry.counter(counterName);
    }

    @Override
    public Health health() {
//        todo create counters for visit succes and visist failed
        Counter dnsVisitsCompleted = getCounter("dns.visits.processed");
        Counter dnsVisitsFailed = getCounter("dns.visits.failed");

        double visitsFailed = dnsVisitsFailed.count();
        double visitsCompleted = dnsVisitsCompleted.count();

        double failureRate = visitsFailed / (visitsCompleted + visitsFailed);

        if (Double.isNaN(failureRate) || Double.compare(failureRate, 0.4) > 0) {
            return Health.down().withDetail("Failure Rate", failureRate).build();
        } else {
            return Health.up().withDetail("Failure rate: ", failureRate).build();
        }
    }

//        Optional<Timer> dnsResolveAll = Optional.ofNullable(Metrics.globalRegistry
//                .find("dns.crawler.resolve.all").timer());
////        not 100% sure about this ones type
//        Optional<Timer> dnsGeoEnrich = Optional.ofNullable(Metrics.globalRegistry
//                .find("dns.crawler.geo.enrich").timer());
//        Optional<Timer> dnsLookUpDone = Optional.ofNullable(Metrics.globalRegistry
//                .find("dns.resolver.lookup.done").timer());
//        Optional<Counter> dnsRecordFound = Optional.ofNullable(Metrics.globalRegistry
//                .find("dns.resolver.records.found").counter());
//        Optional<Gauge> dnsConcurrentCalls = Optional.ofNullable(Metrics.globalRegistry
//                .find("dns.resolver.concurrent.calls").gauge());
}

