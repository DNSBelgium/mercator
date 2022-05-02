package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toList;

@Component
public class DnsResolver {

  private static final Logger logger = LoggerFactory.getLogger(DnsResolver.class);
  private final MeterRegistry meterRegistry;
  private final AtomicInteger concurrentCalls;

  @Value("${resolver.hostname:#{null}}")
  private String hostName;

  @Value("${resolver.port:53}")
  private int port;

  @Value("${resolver.timeout.seconds:10}")
  private int timeoutSeconds;

  public DnsResolver(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.concurrentCalls = meterRegistry.gauge(MetricName.DNS_RESOLVER_CONCURRENT_CALLS, new AtomicInteger(0));

    Lookup.getDefaultCache(DClass.IN).setMaxCache(0);
    Lookup.getDefaultCache(DClass.IN).setMaxNCache(0);
    Lookup.getDefaultCache(DClass.IN).setMaxEntries(0);
  }

  public Records getAllRecords(Name name, List<RecordType> recordTypes) {
    logger.info("Resolving DNS records {} for {}", recordTypes, name);
    return recordTypes
        .stream()
        .map(recordType -> getAllRecords(name, recordType))
        .collect(Collector.of(Records::new, Records::add, Records::add));
  }

  private Records getAllRecords(Name name, RecordType recordType) {
    //TODO: investigate possible issues during static initialization of Lookup class
    //  and, if needed, use other method
    //TODO: ensure proper timeout handling (we should timeout faster than messaging does)
    //  a lookup might involve multiple connections, and there's only a timeout
    //  per connection, no global timeout on a lookup in dnsjava
    // Lookup lookup = createLookup(fullyQualifiedName, recordType);
    logger.debug("Resolving DNS records {} for {}", recordType, name);
    Lookup lookup = performLookup(name, recordType);

    if (lookup.getResult() != Lookup.SUCCESSFUL) {
      // TODO: We should return null when it is not successful
      logger.debug("No result for lookup {} for {}, dnsjava told us: {}", recordType, name, lookup.getErrorString());
    }

    return new Records(Map.of(recordType, getRecords(lookup)));
  }

  private List<RRecord> getRecords(Lookup lookup) {
    Record[] records = lookup.getAnswers();
    if (records == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(records).map(record -> RRecord.of(record.getTTL(), record.rdataToString())).collect(toList());
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
    lookup.setResolver(createResolver());
    return lookup;
  }

  private Resolver createResolver() {
    try {
      SimpleResolver simpleResolver = new SimpleResolver(hostName);
      simpleResolver.setPort(port);
      simpleResolver.setTimeout(timeoutSeconds);
      return simpleResolver;
    } catch (UnknownHostException e) {
      logger.error("Failed to create SimpleResolver", e);
      throw new RuntimeException(e);
    }
  }

  public DnsResolution performCheck(Name fqdn) {
    logger.debug("Performing check for {}", fqdn);
    Lookup testLookup = performLookup(fqdn, RecordType.A);
    int rcode = testLookup.getResult();
    if (rcode == Lookup.UNRECOVERABLE || rcode == Lookup.TRY_AGAIN) {
      logger.debug("Problem during resolution for {}, dnsjava told us: {}", fqdn, testLookup.getErrorString());
      return DnsResolution.failed(rcode, testLookup.getErrorString()).addRecords("@", new Records(Map.of(RecordType.A, getRecords(testLookup))));
    }
    if (rcode == Lookup.HOST_NOT_FOUND) {
      logger.debug("Resolution for {} finished, nxdomain", fqdn);
      return DnsResolution.nxdomain().addRecords("@", new Records(Map.of(RecordType.A, getRecords(testLookup))));
    }
    return DnsResolution.withRecords("@", new Records(Map.of(RecordType.A, getRecords(testLookup))));
  }
}
