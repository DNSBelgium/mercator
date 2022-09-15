package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.slf4j.LoggerFactory.getLogger;

public class DnsResolutionTest {

  private static final Logger logger = getLogger(DnsResolutionTest.class);

  @Test
  void withRecords() {
    var dnsResolution = DnsResolution.withRecords("dnsbelgium.be", "@", RecordsTest.dnsBelgiumRootRecords());
    assertEquals(dnsResolution.getRecords("@"), RecordsTest.dnsBelgiumRootRecords());
  }

  @Test
  void change() {
    var dnsResolution = DnsResolution.withRecords("dnsbelgium.be", "@", RecordsTest.dnsBelgiumRootRecords());
    logger.info("dnsResolution = {}", dnsResolution);

    Records records = new Records(Map.of(
        RecordType.MX, new RRSet(Set.of(RRecord.of(3600L, "0 dnsbelgium-be.mail.protection.outlook.com.")), 0)
    ));

    dnsResolution.getRecords().put("X", records);
    logger.info("dnsResolution = {}", dnsResolution);

    dnsResolution.getRecords().clear();
    logger.info("dnsResolution = {}", dnsResolution);
  }

  @Test
  void addRecords() {
    var dnsResolution = DnsResolution.withRecords("dnsbelgium.be", "@", RecordsTest.dnsBelgiumRootRecords());
    dnsResolution.addRecords("@", new Records(Map.of(RecordType.A, new RRSet(Set.of(RRecord.of(3600L, "192.168.0.1")), 0))));
    assertTrue(dnsResolution.getRecords("@").get(RecordType.A).records().contains(RRecord.of(3600L, "192.168.0.1")));

    assertNull(dnsResolution.getRecords("www"));
    dnsResolution.addRecords("www", RecordsTest.dnsBelgiumWwwRecords());
    assertEquals(dnsResolution.getRecords("www"), RecordsTest.dnsBelgiumWwwRecords());
  }

  @Test
  void isOk() {
    var dnsResolution = DnsResolution.withRecords("dnsbelgium.be", "@", RecordsTest.dnsBelgiumRootRecords());
    assertTrue(dnsResolution.isOk());
  }

  @Test
  void getHumanReadableProblem() {
    var dnsResolution = DnsResolution.withRecords("dnsbelgium.be", "@", RecordsTest.dnsBelgiumRootRecords());
    assertNull(dnsResolution.getHumanReadableProblem());
  }

  @Test
  void failed() {
    var failed = DnsResolution.failed("dnsbelgium.be","@");

    assertFalse(failed.isOk());
    assertEquals(failed.getHumanReadableProblem(), "for whatever reason");
    assertTrue(failed.getRecords().isEmpty());
  }

  @Test
  void testAddRecords() {
    var dnsResolution = DnsResolution.withRecords("dnsbelgium.be", "www", RecordsTest.dnsBelgiumWwwRecords());
    dnsResolution.addRecords("www", new Records(Map.of(
        RecordType.TXT, new RRSet(Set.of(RRecord.of(3600, "foo")), 0)
    )));

    assertThat(dnsResolution.getRecords("@")).isNull();
    assertThat(dnsResolution.getRecords("www")).isEqualTo(
        Records.merge(RecordsTest.dnsBelgiumWwwRecords(), new Records(RecordType.TXT, new RRSet(Set.of(RRecord.of(3600, "foo")), 0)))
    );

    dnsResolution.addRecords("@", new Records(Map.of(
        RecordType.TXT, new RRSet(Set.of(RRecord.of(3600, "bar")), 0)
    )));

    assertThat(dnsResolution.getRecords("www")).isEqualTo(
        Records.merge(RecordsTest.dnsBelgiumWwwRecords(), new Records(RecordType.TXT, new RRSet(Set.of(RRecord.of(3600, "foo")), 0)))
    );
    assertThat(dnsResolution.getRecords("@")).isEqualTo(
        new Records(RecordType.TXT, new RRSet(Set.of(RRecord.of(3600, "bar")), 0))
    );
  }

  // Object Mother

  public static DnsResolution dnsBelgiumDnsResolution() {
    return DnsResolution
        .withRecords("dnsbelgium.be", "@", RecordsTest.dnsBelgiumRootRecords())
        .addRecords("www", RecordsTest.dnsBelgiumWwwRecords())
        .addRecords("_dmarc", RecordsTest.dnsBelgiumDmarcRecords());
  }
}
