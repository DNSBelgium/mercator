package be.dnsbelgium.mercator.dns.health;

import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.dto.DnsRequest;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.Optional;

/**
 * Checks whether we are able to resolve a given domain and obtain several records for it.
 */
@Component
public class ResolverHealthIndicator implements HealthIndicator {
    @Autowired
    private final DnsResolver resolver;

    private final Name name;

    ResolverHealthIndicator(DnsResolver resolver, @Value("${crawler.dns.health.domain}") String healthIndicatorDomain) throws Exception {
        this.resolver = resolver;
        try {
            this.name = Name.fromString(healthIndicatorDomain);
        } catch (TextParseException tpe) {
            throw new Exception("failed to parse given health check domain for DnsCrawler");
        }
    }

    private Optional<String> performLookupTest(String prefix, RecordType type) {
        DnsRequest aLookup = this.resolver.lookup("@", this.name, type);
        if (!aLookup.isOk())
            return Optional.empty();
        if (aLookup.records().size() == 0)
            return Optional.empty();

        return Optional.of(aLookup.records().get(0).getData());
    }

    /**
     * Returns a valid IPv4 if the test worked. Returns empty Optional otherwise.
     */
    private Optional<String> performALookupTest() {
        return performLookupTest("@", RecordType.A);
    }

    /**
     * Returns a valid IPv6 if the test worked. Returns empty Optional otherwise.
     */
    private Optional<String> performAAAALookupTest() {
        return performLookupTest("@", RecordType.AAAA);
    }

    /**
     * Returns a valid IPv6 if the test worked. Returns empty Optional otherwise.
     */
    private Optional<String> performMXLookupTest() {
        return performLookupTest("@", RecordType.MX);
    }

    @Override
    public Health health() {
        Optional<String> aLookupTest = this.performALookupTest();
        boolean aLookupTestSuccess = aLookupTest.isPresent();

        Optional<String> aaaaLookupTest = this.performAAAALookupTest();
        boolean aaaaLookupTestSuccess = aaaaLookupTest.isPresent();

        Optional<String> mxLookupTest = this.performMXLookupTest();
        boolean mxLookupTestSuccess = mxLookupTest.isPresent();

        Health.Builder builder = aLookupTestSuccess && aaaaLookupTestSuccess && mxLookupTestSuccess ? Health.up() : Health.down();

        // TODO: add geoIP health status

        builder.withDetail("aLookup", aLookupTest.orElse(""));
        builder.withDetail("aaaaLookup", aaaaLookupTest.orElse(""));
        builder.withDetail("mxLookup", mxLookupTest.orElse(""));

        return builder.build();
    }
}
