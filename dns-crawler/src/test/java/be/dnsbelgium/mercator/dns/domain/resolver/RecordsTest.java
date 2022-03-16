package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    Records record = new Records(Map.of(RecordType.A, List.of("1.2.3.4", "1.1.1.1"), RecordType.SOA, Collections.singletonList("ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")));
    assertNotNull(record.getRecords());
    assertFalse(record.getRecords().isEmpty());
    assertEquals(record.getRecords().size(), 2);
  }

  @Test
  void get() {
    Records record = new Records(Map.of(RecordType.A, List.of("1.2.3.4", "1.1.1.1"), RecordType.SOA, List.of("ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")));

    assertEquals(record.get(RecordType.A), List.of("1.2.3.4", "1.1.1.1"));
    assertEquals(record.get(RecordType.AAAA), Collections.emptyList());
  }

  @Test
  void add() {
    Records record = new Records(Map.of(RecordType.A, List.of("1.2.3.4", "1.1.1.1"), RecordType.SOA, List.of("ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600")));
    record.add(new Records(Map.of(RecordType.AAAA, List.of("2a02:e980:53::8b"))));

    assertEquals(record.get(RecordType.A), List.of("1.2.3.4", "1.1.1.1"));
    assertEquals(record.get(RecordType.AAAA), List.of("2a02:e980:53::8b"));

    record.add(new Records(Map.of(RecordType.A, List.of("1.2.3.5"))));
    assertEquals(record.get(RecordType.A), List.of("1.2.3.4", "1.1.1.1", "1.2.3.5"));
  }

  @Test
  void addDuplicate() {
    Records record = new Records(Map.of(RecordType.A, List.of("1.2.3.4", "1.1.1.1")));

    assertEquals(record.get(RecordType.A), List.of("1.2.3.4", "1.1.1.1"));

    record.add(new Records(Map.of(RecordType.A, List.of("1.2.3.5", "1.1.1.1"))));
    assertEquals(record.get(RecordType.A), List.of("1.2.3.4", "1.1.1.1", "1.2.3.5", "1.1.1.1"));
  }

  @Test
  void getRecords() {
    var map = Map.of(RecordType.A, List.of("1.2.3.4", "1.1.1.1"), RecordType.SOA, List.of("ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144636 10800 1800 3600000 3600"));

    Records record = new Records(map);
    assertEquals(record.getRecords(), map);
  }

  // Object Mothers

  public static Records dnsBelgiumDmarcRecords() {
    return new Records(Map.of(RecordType.TXT, List.of("v=DMARC1; p=quarantine; pct=10; fo=0; rua=mailto:dmarc@dnsbelgium.be; ruf=mailto:dmarc@dnsbelgium.be")));
  }

  public static Records dnsBelgiumWwwRecords() {
    return new Records(Map.of(
        RecordType.AAAA, List.of("2a02:e980:53:0:0:0:0:8b"),
        RecordType.A, List.of("107.154.248.139")
    ));
  }

  public static Records dnsBelgiumRootRecords() {
    return new Records(Map.of(
        RecordType.MX, List.of("0 dnsbelgium-be.mail.protection.outlook.com."),
        RecordType.AAAA, List.of("2a02:e980:53:0:0:0:0:8b"),
        RecordType.SOA, List.of("ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144915 10800 1800 3600000 3600"),
        RecordType.TXT, List.of(
            "\"QHW9u39wLyjqPCFmoNpDsDJHubOneJ2Eecw5Xt+DljI=\"",
            "\"kfNdt0RkyOLOHg+hmXkon6UdyujBTRZnDsrB21shytM=\"",
            "\"spf2.0/mfrom,pra include:spf.protection.outlook.com include:qlan.eu include:servers.mcsv.net include:spf.flexmail.eu ip4:52.17.217.28 ip4:52.214.17.58 ip4:84.199.48.136 -all\"",
            "\"v=spf1 include:spf.protection.outlook.com include:qlan.eu include:_spf.elasticemail.com include:servers.mcsv.net include:spf.flexmail.eu ip4:52.17.217.28 ip4:52.214.17.58 ip4:84.199.48.136 -all\""
        ),
        RecordType.A, List.of("107.154.248.139"),
        RecordType.CAA, List.of(
            "0 issue \"letsencrypt.org\"",
            "0 issue \"globalsign.com\"",
            "0 issue \"amazon.com\"",
            "0 iodef \"mailto:cert-abuse@dnsbelgium.be\""
        )
    ));
  }
}
