package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.DnsRequest;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.xbill.DNS.Lookup.UNRECOVERABLE;

@Component
public class DnsResolver {

  private static final Logger logger = LoggerFactory.getLogger(DnsResolver.class);
  private final MeterRegistry meterRegistry;
  // used for metrics only
  private final AtomicInteger concurrentCalls;

  @Value("${resolver.hostname:#{null}}")
  private String hostName;

  @Value("${resolver.port:53}")
  private int port;

  @Value("${resolver.tcp:false}")
  private boolean tcp = false;

  @Value("${resolver.timeout.seconds:10}")
  private int timeoutSeconds;

  @Autowired
  public DnsResolver(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.concurrentCalls = meterRegistry.gauge(MetricName.DNS_RESOLVER_CONCURRENT_CALLS, new AtomicInteger(0));
    Lookup.getDefaultCache(DClass.IN).setMaxCache(0);
    Lookup.getDefaultCache(DClass.IN).setMaxNCache(0);
    Lookup.getDefaultCache(DClass.IN).setMaxEntries(0);
  }

  public DnsRequest lookup(String prefix, Name name, RecordType recordType) {
    var start = LocalDateTime.now();
    try {
      concurrentCalls.incrementAndGet();
      Name ownerName = Name.fromString(prefix, name);
      logger.info("Resolving DNS records of type {} for name {}", recordType, ownerName);
      Lookup lookup = initLookup(ownerName, recordType);
      lookup.run();
      updateMetrics(recordType, lookup);
      int rcode = lookup.getResult();
      Record[] answers = lookup.getAnswers();

      List<RRecord> records = new ArrayList<>();
      int expectedType = Type.value(recordType.name());
      if (answers != null) {
        for (Record answer : answers) {
          if (answer.getType() == expectedType) {
            records.add(RRecord.of(answer.getTTL(), answer.rdataToString()));
          } else {
            logger.error("Type of answer {} does not match requested recordType {} => ignoring", answer.getType(),recordType);
          }
        }
      }
      String error = lookup.getErrorString();
      String problem = ("successful".equals(error)) ? null : error;
      DnsRequest request = new DnsRequest(prefix, recordType, rcode, problem, records);
      if (lookup.getResult() != Lookup.SUCCESSFUL) {
        logger.debug("No result for lookup {} for {}, dnsjava told us: {}", recordType, name, lookup.getErrorString());
      }
      return request;
      
    } catch (TextParseException e) {
      var problem = "Prefix %s for %s is invalid".formatted(prefix, name);
      logger.error(problem, e);
      return new DnsRequest(prefix, recordType, UNRECOVERABLE, problem, List.of());
    } finally {
      var end = LocalDateTime.now();
      Timer timer = meterRegistry.timer(MetricName.DNS_RESOLVER_LOOKUP_DONE, tagFor(recordType));
      timer.record(Duration.between(start, end));
      concurrentCalls.decrementAndGet();
    }
  }

  private Tags tagFor(RecordType recordType) {
    return Tags.of("type", recordType.name());
  }

  private void updateMetrics(RecordType recordType, Lookup lookup) {
    Tags tags = tagFor(recordType);
    Record[] answers = lookup.getAnswers();
    int recordCount = (answers == null) ? 0 : answers.length;
    tags = tags.and("resultCode", Rcode.string(lookup.getResult()));
    tags = tags.and("recordsFound", recordCount > 0 ? "yes" : "no");
    meterRegistry.counter(MetricName.DNS_RESOLVER_RECORDS_FOUND, tags).increment(recordCount);
  }

  private Lookup initLookup(Name name, RecordType recordType) {
    int value = Type.value(recordType.toString());
    Lookup lookup = new Lookup(name, value);
    lookup.setResolver(createResolver());
    return lookup;
  }

  private Resolver createResolver() {
    try {
      logger.info("resolver: {}", hostName);
      SimpleResolver simpleResolver = new SimpleResolver(hostName);
      simpleResolver.setPort(port);
      simpleResolver.setTCP(tcp);
      simpleResolver.setTimeout(Duration.ofSeconds(timeoutSeconds));
      logger.info("Using {}:{}:{}", tcp?"tcp":"udp", hostName, port);
      return simpleResolver;
    } catch (UnknownHostException e) {
      logger.error("Failed to create SimpleResolver", e);
      throw new RuntimeException(e);
    }
  }

}
