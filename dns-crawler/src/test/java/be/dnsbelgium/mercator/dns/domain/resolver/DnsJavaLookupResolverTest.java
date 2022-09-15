package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig({DnsJavaLookupResolver.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
class DnsJavaLookupResolverTest {

  @Autowired
  DnsJavaLookupResolver dnsResolver;

  @Test
  void testGetAllRecords() {
    DnsResolution dnsResolution = dnsResolver.getAllRecords("dnsbelgium.be", "@", Set.of(RecordType.SOA, RecordType.AAAA, RecordType.CAA));
    assertNotNull(dnsResolution);

    System.out.println(dnsResolution);
    assertNull(dnsResolution.getRecords("@").get(RecordType.A));
    assertThat(dnsResolution.getRecords("@").get(RecordType.SOA).records()).isNotEmpty();
    assertThat(dnsResolution.getRecords("@").get(RecordType.AAAA).records()).isNotEmpty();
    assertThat(dnsResolution.getRecords("@").get(RecordType.CAA).records()).isNotEmpty();

    assertTrue(dnsResolution.getRecords("@").get(RecordType.SOA).records().stream().map(RRecord::data).collect(Collectors.joining(" ")).contains("be-hostmaster.dnsbelgium.be"));
    assertTrue(dnsResolution.getRecords("@").get(RecordType.AAAA).records().size() > 0);
  }

  @Test
  void testWrongName() {
    DnsResolution dnsResolution = dnsResolver.getAllRecords(".a.be", "@", Set.of(RecordType.SOA, RecordType.AAAA, RecordType.CAA));
    assertNull(dnsResolution);

    dnsResolution = dnsResolver.getAllRecords("a.be", "..", Set.of(RecordType.SOA, RecordType.AAAA, RecordType.CAA));
    assertNull(dnsResolution);
  }

  @Test
  void testIDN() {
    DnsResolution dnsResolution = dnsResolver.getAllRecords("café.be", "@", Set.of(RecordType.SOA, RecordType.AAAA));
    assertNotNull(dnsResolution);

    System.out.println(dnsResolution);

    assertNull(dnsResolution.getRecords("@").get(RecordType.A));
    assertThat(dnsResolution.getRecords("@").get(RecordType.SOA).records()).isNotEmpty();
    assertThat(dnsResolution.getRecords("@").get(RecordType.AAAA).records()).isEmpty();

    assertTrue(dnsResolution.getRecords("@").get(RecordType.SOA).records().stream().map(RRecord::data).collect(Collectors.joining(" ")).contains("dnsadmin.bodis.com."));
  }

}
