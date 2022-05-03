package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig({DnsResolver.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
class DnsResolverTest {

  @Autowired
  DnsResolver dnsResolver;

  @Test
  void testGetAllRecords() throws TextParseException {
    Records allRecords = dnsResolver.getAllRecords(Name.fromString("dnsbelgium.be"), List.of(RecordType.SOA, RecordType.AAAA, RecordType.CAA));
    assertNotNull(allRecords);

    assertThat(allRecords.get(RecordType.A)).isEmpty();
    assertThat(allRecords.get(RecordType.SOA)).isNotEmpty();
    assertThat(allRecords.get(RecordType.AAAA)).isNotEmpty();
    assertThat(allRecords.get(RecordType.CAA)).isNotEmpty();

    assertTrue(allRecords.get(RecordType.SOA).stream().map(RRecord::getData).collect(Collectors.joining(" ")).contains("be-hostmaster.dnsbelgium.be"));
    assertTrue(allRecords.get(RecordType.AAAA).size() > 0);
  }

  @Test
  void performCheck() throws TextParseException {
    DnsResolution dnsResolution = dnsResolver.performCheck(Name.fromString("dnsbelgium.be"));
    assertTrue(dnsResolution.isOk());
    assertNotNull(dnsResolution.getRecords());
    assertNotNull(dnsResolution.getRecords("@"));
    assertNotNull(dnsResolution.getRecords("@").get(RecordType.A));
  }

}
