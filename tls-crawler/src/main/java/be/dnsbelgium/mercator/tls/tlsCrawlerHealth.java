package be.dnsbelgium.mercator.tls;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class tlsCrawlerHealth implements HealthIndicator {
    @Override
    public Health health() {
        boolean contentCrawlerHealthParameters = true;

        /**
         * what needs to be true to give back up or otherwise
         * database already checked done.
         * queue connected ?
         *
         */

        Health.Builder status = Health.up();
        if (!contentCrawlerHealthParameters) {
            status = Health.down().withDetail("reason", "details about why it failed");

        }
        return status.build();
    }
}
