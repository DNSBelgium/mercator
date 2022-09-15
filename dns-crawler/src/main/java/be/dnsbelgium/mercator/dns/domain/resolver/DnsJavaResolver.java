package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.*;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
public class DnsJavaResolver implements DnsResolver {

  private static final Logger logger = LoggerFactory.getLogger(DnsJavaResolver.class);
  private final MeterRegistry meterRegistry;
  private final AtomicInteger concurrentCalls;

  private final Resolver resolver;

  public DnsJavaResolver(MeterRegistry meterRegistry,
                         @Value("${resolver.hostname:#{null}}") String hostname,
                         @Value("${resolver.port:53}") int port,
                         @Value("${resolver.timeout.seconds:10}") int timeoutSeconds) {
    this.resolver = DnsJavaUtil.createResolver(hostname, port, timeoutSeconds);

    this.meterRegistry = meterRegistry;
    this.concurrentCalls = meterRegistry.gauge(MetricName.DNS_RESOLVER_CONCURRENT_CALLS, new AtomicInteger(0));
  }

  @Override
  public DnsResolution getAllRecords(String domainName, String prefix, Set<RecordType> recordTypes) {
    Name fqdn = DnsJavaUtil.getNameFromSubdomain(domainName, prefix);
    if (fqdn == null) return null; // Cannot parse the fqdn

    return getAllRecords(domainName, prefix, fqdn, recordTypes);
  }

  private DnsResolution getAllRecords(String domainName, String prefix, Name fqdn, Set<RecordType> recordTypes) {
    logger.info("Resolving DNS records {} for {}", recordTypes, fqdn);

    Optional<RecordType> firstRecordType = recordTypes.stream().findFirst();
    if (firstRecordType.isEmpty()) {
      return DnsResolution.empty(domainName, prefix);
    }

    // Performing a check first
    DnsResolution dnsResolution = performCheck(domainName, prefix, fqdn, firstRecordType.get());

    if (!dnsResolution.isOk()) {
      return dnsResolution;
    }

    Records records = recordTypes
        .stream()
        .filter(recordType -> recordType != firstRecordType.get()) // Ignore already queried records in performCheck
        .map(recordType -> new Records(recordType, getAllRecords(fqdn, recordType)))
        .collect(Collector.of(Records::new, Records::add, Records::add));

    dnsResolution.addRecords(prefix, records);

    return dnsResolution;
  }

  private RRSet getRecords(Message message) {
    Set<RRecord> rRecords = message.getSectionRRsets(Section.ANSWER)
        .stream()
        .map(RRset::rrs)
        .flatMap(Collection::stream)
        .map(record -> RRecord.of(record.getTTL(), record.rdataToString()))
        .collect(Collectors.toSet());

    return new RRSet(rRecords, message.getRcode());
  }

  private Message performLookup(Name name, RecordType recordType) throws IOException {
    concurrentCalls.incrementAndGet();

    Tags tags = Tags.of("type", recordType.name());
    var start = LocalDateTime.now();
    try {
      Record question = Record.newRecord(name, Type.value(recordType.toString()), DClass.IN);
      Message query = Message.newQuery(question);
      Message response = resolver.send(query);

      int recordCount = response.getSectionRRsets(Section.ANSWER).size();
      tags = tags.and("resultCode", Rcode.string(response.getRcode()));
      meterRegistry.counter(MetricName.DNS_RESOLVER_RECORDS_FOUND, tags).increment(recordCount);
      tags = tags.and("recordsFound", recordCount > 0 ? "yes" : "no");

      return response;
    } finally {
      var end = LocalDateTime.now();
      Timer timer = meterRegistry.timer(MetricName.DNS_RESOLVER_LOOKUP_DONE, tags);
      timer.record(Duration.between(start, end));
      concurrentCalls.decrementAndGet();
    }
  }

  private RRSet getAllRecords(Name name, RecordType recordType) {
    logger.debug("Resolving DNS records {} for {}", recordType, name);
    Message message;
    try {
      message = performLookup(name, recordType);
    } catch (IOException e) {
      logger.warn("Cannot perform lookup", e);
      return null;
    }

    return getRecords(message);
  }

  public DnsResolution performCheck(String domainName, String prefix, Name fqdn, RecordType recordType) {
    logger.debug("Performing check for {}", fqdn);
    Message message;
    try {
      message = performLookup(fqdn, recordType);
    } catch (SocketTimeoutException e) {
      return DnsResolution.timeout(domainName, prefix);
    } catch (IOException e) {
      logger.error("Network error", e);
      return DnsResolution.networkError(domainName, prefix);
    }
    int rcode = message.getRcode();
    switch (rcode) {
      case Rcode.NOERROR:
        return DnsResolution.withRecords(domainName, prefix, new Records(Map.of(recordType, getRecords(message))));
      case Rcode.NXDOMAIN:
        return DnsResolution.nxdomain(domainName, prefix, new Records(Map.of(RecordType.A, getRecords(message)))).addRecords(prefix, new Records(Map.of(recordType, getRecords(message))));
      default:
        logger.error("Something unplanned just happened. rcode {} {}", rcode, Rcode.string(rcode));
        return DnsResolution.failed(domainName, prefix);
    }
  }

}
