package be.dnsbelgium.mercator.dns.dto;

import java.util.Collections;
import java.util.List;

/**
 * Represents a DNS request and its responses
 */
public record DnsRequest(String prefix, RecordType recordType, int rcode, String humanReadableProblem,
                         List<RRecord> records) {
    public static DnsRequest success(String prefix, RecordType type, RRecord record) {
        return new DnsRequest(prefix, type, 0, null, List.of(record));
    }

    public static DnsRequest success(String prefix, RecordType type, List<RRecord> records) {
        return new DnsRequest(prefix, type, 0, null, records);
    }

    public static DnsRequest nxdomain(String prefix, RecordType type) {
        return new DnsRequest(prefix, type, 3, "nxdomain", Collections.emptyList());
    }

    public boolean isOk() {
        // TODO: discuss with team when we should set the "ok" column to false.
        // Only when we get a SERVFAIL ?
        return rcode == 0;
    }
}
