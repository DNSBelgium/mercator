package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.shaded.org.bouncycastle.util.IPAddress;
import org.xbill.DNS.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig({DnsResolver.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
// use TCP to avoid annoying firewalls from breaking the tests
@TestPropertySource(properties = {"resolver.hostname=8.8.8.8", "resolver.tcp=true"})
class DnsResolverTest {

  /*
    This test does actual DNS requests for dnsbelgium.be, so the results depend on how this zone is configured
   */

  @Autowired
  DnsResolver dnsResolver;

  private static final Logger logger = LoggerFactory.getLogger(DnsResolverTest.class);

  @Test
  public void lookup_apex_A() throws TextParseException {
    DnsRequest request = atLeastOneRecordFoundOfType("@", RecordType.A);
    for (RRecord record : request.records()) {
      assertRecordContainsValidIp(record);
    }
  }
  @Test
  public void lookup_apex_AAAA() throws TextParseException {
    DnsRequest request = atLeastOneRecordFoundOfType("@", RecordType.AAAA);
    for (RRecord record : request.records()) {
      assertRecordContainsValidIp(record);
    }
  }

  @Test
  public void lookupApexDnsBelgium() throws TextParseException {
    atLeastOneRecordFoundOfType("@", RecordType.CAA);
    atLeastOneRecordFoundOfType("@", RecordType.A);
    atLeastOneRecordFoundOfType("@", RecordType.AAAA);
    atLeastOneRecordFoundOfType("@", RecordType.SOA);
    atLeastOneRecordFoundOfType("@", RecordType.TXT);

  }

  @Test
  //@Disabled // works locally but fails on Jenkins ?? Let's first fix rest of the build
  public void nxdomain() throws TextParseException {
    Name dnsbelgium = Name.fromString("this-domain-is-not-registered.dnsbelgium.be.");
    DnsRequest request = dnsResolver.lookup("nxdomain", dnsbelgium, RecordType.A);
    logger.info("nxdomain: request = {}", request);
    assertThat(request.rcode()).withFailMessage("Expected NXDOMAIN").isEqualTo(Rcode.NXDOMAIN);
    assertThat(request.recordType()).isEqualTo(RecordType.A);
    assertThat(request.records()).isEmpty();
    assertThat(request.humanReadableProblem()).isEqualTo("host not found");
  }

  @Test
  public void _domainkey_found() throws TextParseException {
    // we have a TXT record at _domainkey.dnsbelgium.be
    atLeastOneRecordFoundOfType("_domainkey", RecordType.TXT);
  }

  @Test
  @Disabled
  //todo: find out why 8.8.8.8 seems to respond differently on some machines
  // but still returns NXDOMAIN when using dig ...
  public void _domainkey_not_found() throws TextParseException {
    // at the moment we have no records under abc.dns.be
    Name parent = Name.fromString("abc.dns.be");
    DnsRequest request = dnsResolver.lookup("_domainkey", parent, RecordType.TXT);
    logger.info("_domainkey_not_found: request = {}", request);
    assertThat(request.rcode()).isEqualTo(Rcode.NXDOMAIN);
    assertThat(request.recordType()).isEqualTo(RecordType.TXT);
    assertThat(request.records()).isEmpty();
    assertThat(request.humanReadableProblem()).isEqualTo("host not found");
  }

  private void assertRecordContainsValidIp(RRecord record) {
    String rdata = record.getData();
    assertThat(IPAddress.isValid(rdata))
            .withFailMessage("rdata should be an IP address but was " + rdata)
            .isTrue();
  }

  private DnsRequest atLeastOneRecordFoundOfType(String prefix, RecordType recordType) throws TextParseException {
    Name dnsbelgium = Name.fromString("dnsbelgium.be");
    DnsRequest request = dnsResolver.lookup(prefix, dnsbelgium, recordType);
    logger.info("request = {}", request);
    assertThat(request.rcode()).isEqualTo(0);
    assertThat(request.recordType()).isEqualTo(recordType);
    assertThat(request.records()).hasSizeGreaterThanOrEqualTo(1);
    return request;
  }

}
