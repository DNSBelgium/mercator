package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.*;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
@Primary // TODO Make it conditional
public class DnsJavaLookupResolver implements DnsResolver {

  private static final Logger logger = LoggerFactory.getLogger(DnsResolver.class);
  private final MeterRegistry meterRegistry;
  private final AtomicInteger concurrentCalls;

  @Value("${resolver.hostname:#{null}}")
  private String hostName;

  @Value("${resolver.port:53}")
  private int port;

  @Value("${resolver.timeout.seconds:10}")
  private int timeoutSeconds;

  public DnsJavaLookupResolver(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.concurrentCalls = meterRegistry.gauge(MetricName.DNS_RESOLVER_CONCURRENT_CALLS, new AtomicInteger(0));

    Lookup.getDefaultCache(DClass.IN).setMaxCache(0);
    Lookup.getDefaultCache(DClass.IN).setMaxNCache(0);
    Lookup.getDefaultCache(DClass.IN).setMaxEntries(0);
  }

  private DnsResolution getAllRecords(DnsResolution dnsResolution, String prefix, Name fqdn, Set<RecordType> recordTypes) {
    logger.info("Resolving DNS records {} for {}", recordTypes, dnsResolution.getDomainName());

    Records rRecords = recordTypes
        .stream()
        .map(recordType -> new Records(recordType, getAllRecords(fqdn, recordType)))
        .collect(Collector.of(Records::new, Records::add, Records::add));

    dnsResolution.addRecords(prefix, rRecords);

    return dnsResolution;
  }

  private RRSet getAllRecords(Name name, RecordType recordType) {
    //TODO: investigate possible issues during static initialization of Lookup class
    //  and, if needed, use other method
    //TODO: ensure proper timeout handling (we should timeout faster than messaging does)
    //  a lookup might involve multiple connections, and there's only a timeout
    //  per connection, no global timeout on a lookup in dnsjava
    // Lookup lookup = createLookup(fullyQualifiedName, recordType);
    logger.debug("Resolving DNS records {} for {}", recordType, name);
    Lookup lookup = performLookup(name, recordType);

    if (lookup.getResult() != Lookup.SUCCESSFUL) {
      logger.debug("No result for lookup {} for {}, dnsjava told us: {}", recordType, name, lookup.getErrorString());
    }

    return getRecords(lookup);
  }

  private Integer getRocdeFromLookupResult(int result) {
    return switch (result) {
      case Lookup.SUCCESSFUL -> Rcode.NOERROR;
      case Lookup.HOST_NOT_FOUND -> Rcode.NXDOMAIN;
      case Lookup.UNRECOVERABLE, Lookup.TRY_AGAIN, Lookup.TYPE_NOT_FOUND -> null;
      default -> {
        logger.error("Cannot map Lookup result code.");
        throw new IllegalArgumentException("Cannot map Lookup result code.");
      }
    };
  }

  private RRSet getRecords(Lookup lookup) {
    Integer rcode = getRocdeFromLookupResult(lookup.getResult());
    Record[] records = lookup.getAnswers();
    if (rcode == null || records == null) {
      return new RRSet(Collections.emptySet(), rcode);
    }
    return new RRSet(Arrays.stream(records).map(record -> RRecord.of(record.getTTL(), record.rdataToString())).collect(Collectors.toSet()), rcode);
  }

  private Lookup performLookup(Name name, RecordType recordType) {
    concurrentCalls.incrementAndGet();

    Tags tags = Tags.of("type", recordType.name());
    var start = LocalDateTime.now();
    try {
      Lookup lookup = initLookup(name, recordType);

      lookup.run();

      Record[] records = lookup.getAnswers();
      int recordCount = (records == null) ? 0 : records.length;
      tags = tags.and("resultCode", Rcode.string(lookup.getResult()));
      meterRegistry.counter(MetricName.DNS_RESOLVER_RECORDS_FOUND, tags).increment(recordCount);
      tags = tags.and("recordsFound", recordCount > 0 ? "yes" : "no");

      return lookup;
    } finally {
      var end = LocalDateTime.now();
      Timer timer = meterRegistry.timer(MetricName.DNS_RESOLVER_LOOKUP_DONE, tags);
      timer.record(Duration.between(start, end));
      concurrentCalls.decrementAndGet();
    }
  }

  private Lookup initLookup(Name name, RecordType recordType) {
    int value = Type.value(recordType.toString());
    Lookup lookup;
    lookup = new Lookup(name, value);
    lookup.setResolver(DnsJavaUtil.createResolver(hostName, port, timeoutSeconds));
    return lookup;
  }

  public DnsResolution performCheck(Name fqdn) {
    logger.debug("Performing check for {}", fqdn);
    Lookup testLookup = performLookup(fqdn, RecordType.A);
    int rcode = testLookup.getResult();
    if (rcode == Lookup.UNRECOVERABLE || rcode == Lookup.TRY_AGAIN) {
      logger.debug("Problem during resolution for {}, dnsjava told us: {}", fqdn, testLookup.getErrorString());
      return DnsResolution.failed(null, testLookup.getErrorString()).addRecords("@", new Records(Map.of(RecordType.A, getRecords(testLookup))));
    }
    if (rcode == Lookup.HOST_NOT_FOUND) {
      logger.debug("Resolution for {} finished, nxdomain", fqdn);
      return DnsResolution.nxdomain("", "@", new Records(Map.of(RecordType.A, getRecords(testLookup))));
    }
    return DnsResolution.withRecords("", "@", new Records(Map.of(RecordType.A, getRecords(testLookup))));
  }

  @Override
  public DnsResolution getAllRecords(String domainName, String prefix, Set<RecordType> recordTypes) {
    Name fqdn = DnsJavaUtil.getNameFromSubdomain(domainName, prefix);
    if (fqdn == null) return null; // Cannot parse the fqdn

    // First perform a lookup for A records
    DnsResolution dnsResolution = performCheck(fqdn);

    // If dnsResolution is not ok then we save the failed request to the DB, so we know it has been requested.
    if (!dnsResolution.isOk()) {
      return dnsResolution;
    }

    Set<RecordType> remainingRecordTypes = recordTypes.stream()
        .filter(recordType -> !dnsResolution.getRecords(prefix).getRecords().containsKey(recordType))
        .collect(Collectors.toSet());

    return getAllRecords(dnsResolution, prefix, fqdn, remainingRecordTypes);
  }

}
