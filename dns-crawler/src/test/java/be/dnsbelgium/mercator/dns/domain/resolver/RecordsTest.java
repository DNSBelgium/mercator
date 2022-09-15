package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.RRSet;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class RecordsTest {

  @Test
  void emptyConstructor() {
    Records record = new Records();
    assertNotNull(record.getRecords());
    assertTrue(record.getRecords().isEmpty());
  }

  @Test
  void parametrizedConstructor() {
    Records record = new Records(Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0), RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")), 0)));
    assertNotNull(record.getRecords());
    assertFalse(record.getRecords().isEmpty());
    assertEquals(record.getRecords().size(), 2);
  }

  @Test
  void get() {
    Records record = new Records(Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0), RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")), 0)));

    assertEquals(record.get(RecordType.A), new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0));
    assertNull(record.get(RecordType.AAAA));
  }

  @Test
  void add() {
    Records record = new Records(Map.of(
        RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0),
        RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")), 0)
    ));
    record.add(Map.of(
        RecordType.AAAA, new RRSet(Set.of(RRecord.of(3600L, "2a02:e980:53::8b")), 0)
    ));

    assertEquals(record.get(RecordType.A), new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0));
    assertEquals(record.get(RecordType.AAAA), new RRSet(Set.of(RRecord.of(3600L, "2a02:e980:53::8b")), 0));

    record.add(Map.of(
        RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.5")), 0)
    ));
    assertEquals(record.get(RecordType.A), new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1"), RRecord.of(3600L, "1.2.3.5")), 0));
  }

  @Test
  void addDuplicate() {
    Records record = new Records(Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0)));

    assertEquals(record.get(RecordType.A), new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0));

    record.add(Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.5"), RRecord.of(3600L, "1.1.1.1")), 0)));
    assertEquals(record.get(RecordType.A), new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1"), RRecord.of(3600L, "1.2.3.5")), 0));
  }

  @Test
  void getRecords() {
    var map = Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0), RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")), 0));

    Records record = new Records(map);
    assertEquals(record.getRecords(), map);
  }

  @Test
  void testMerge() {
    var map1 = Map.of(RecordType.AAAA, new RRSet(Set.of(RRecord.of(3600L, "dacd:005e:312e:d849:fcd6:2198:e499:a303"), RRecord.of(3600L, "12b3:d141:d793:acbe:795d:3d53:8bdf:c4b1")), 0), RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")), 0));
    Records r1 = new Records(map1);

    var map2 = Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0));
    Records r2 = new Records(map2);

    Records merge = Records.merge(r1, r2);
    assertThat(merge.getRecords()).isEqualTo(Map.of(
        RecordType.AAAA, new RRSet(Set.of(RRecord.of(3600L, "dacd:005e:312e:d849:fcd6:2198:e499:a303"), RRecord.of(3600L, "12b3:d141:d793:acbe:795d:3d53:8bdf:c4b1")), 0),
        RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")), 0),
        RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "1.2.3.4"), RRecord.of(3600L, "1.1.1.1")), 0)
    ));
  }

  // Object Mothers

  public static Records dnsBelgiumDmarcRecords() {
    return new Records(Map.of(RecordType.TXT, new RRSet(Set.of(RRecord.of(3600L, "v=DMARC1; p=quarantine; pct=10; fo=0; rua=mailto:dmarc@dnsbelgium.be; ruf=mailto:dmarc@dnsbelgium.be")), 0)));
  }

  public static Records dnsBelgiumWwwRecords() {
    return new Records(Map.of(
        RecordType.AAAA, new RRSet(Set.of(RRecord.of(3600L, "2a02:e980:53:0:0:0:0:8b")), 0),
        RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "107.154.248.139")), 0)
    ));
  }

  public static Records dnsBelgiumRootRecords() {
    return new Records(Map.of(
        RecordType.MX, new RRSet(Set.of(RRecord.of(3600L, "0 dnsbelgium-be.mail.protection.outlook.com.")), 0),
        RecordType.AAAA, new RRSet(Set.of(RRecord.of(3600L, "2a02:e980:53:0:0:0:0:8b")), 0),
        RecordType.SOA, new RRSet(Set.of(RRecord.of(3600L, "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144915 10800 1800 3600000 3600")), 0),
        RecordType.TXT, new RRSet(Set.of(
            RRecord.of(3600L, "\"QHW9u39wLyjqPCFmoNpDsDJHubOneJ2Eecw5Xt+DljI=\""),
            RRecord.of(3600L, "\"kfNdt0RkyOLOHg+hmXkon6UdyujBTRZnDsrB21shytM=\""),
            RRecord.of(3600L, "\"spf2.0/mfrom,pra include:spf.protection.outlook.com include:qlan.eu include:servers.mcsv.net include:spf.flexmail.eu ip4:52.17.217.28 ip4:52.214.17.58 ip4:84.199.48.136 -all\""),
            RRecord.of(3600L, "\"v=spf1 include:spf.protection.outlook.com include:qlan.eu include:_spf.elasticemail.com include:servers.mcsv.net include:spf.flexmail.eu ip4:52.17.217.28 ip4:52.214.17.58 ip4:84.199.48.136 -all\"")
        ), 0),
        RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "107.154.248.139")), 0),
        RecordType.CAA, new RRSet(Set.of(
            RRecord.of(3600L, "0 issue \"letsencrypt.org\""),
            RRecord.of(3600L, "0 issue \"globalsign.com\""),
            RRecord.of(3600L, "0 issue \"amazon.com\""),
            RRecord.of(3600L, "0 iodef \"mailto:cert-abuse@dnsbelgium.be\"")
        ), 0)
    ));
  }
}
