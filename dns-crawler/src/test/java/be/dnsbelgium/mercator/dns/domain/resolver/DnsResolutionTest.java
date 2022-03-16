package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.slf4j.LoggerFactory.getLogger;

public class DnsResolutionTest {

  private static final Logger logger = getLogger(DnsResolutionTest.class);

  @Test
  void withRecords() {
    var dnsResolution = DnsResolution.withRecords("@", RecordsTest.dnsBelgiumRootRecords());
    assertEquals(dnsResolution.getRecords("@"), RecordsTest.dnsBelgiumRootRecords());
  }

  @Test
  void change() {
    var dnsResolution = DnsResolution.withRecords("@", RecordsTest.dnsBelgiumRootRecords());
    logger.info("dnsResolution = {}", dnsResolution);

    Records records = new Records(Map.of(
        RecordType.MX, List.of("0 dnsbelgium-be.mail.protection.outlook.com.")));

    dnsResolution.getRecords().put("X", records);
    logger.info("dnsResolution = {}", dnsResolution);

    dnsResolution.getRecords().clear();
    logger.info("dnsResolution = {}", dnsResolution);

  }


  @Test
  void addRecords() {
    var dnsResolution = DnsResolution.withRecords("@", RecordsTest.dnsBelgiumRootRecords());
    dnsResolution.addRecords("@", new Records(Map.of(RecordType.A, List.of("192.168.0.1"))));
    assertTrue(dnsResolution.getRecords("@").get(RecordType.A).contains("192.168.0.1"));

    assertNull(dnsResolution.getRecords("www"));
    dnsResolution.addRecords("www", RecordsTest.dnsBelgiumWwwRecords());
    assertEquals(dnsResolution.getRecords("www"), RecordsTest.dnsBelgiumWwwRecords());
  }

  @Test
  void isOk() {
    var dnsResolution = DnsResolution.withRecords("@", RecordsTest.dnsBelgiumRootRecords());
    assertTrue(dnsResolution.isOk());
  }

  @Test
  void getHumanReadableProblem() {
    var dnsResolution = DnsResolution.withRecords("@", RecordsTest.dnsBelgiumRootRecords());
    assertNull(dnsResolution.getHumanReadableProblem());
  }

  @Test
  void failed() {
    var failed = DnsResolution.failed("for whatever reason");

    assertFalse(failed.isOk());
    assertEquals(failed.getHumanReadableProblem(), "for whatever reason");
    assertTrue(failed.getRecords().isEmpty());
  }

  // Object Mother

  public static DnsResolution dnsBelgiumDnsResolution() {
    return DnsResolution
        .withRecords("@", RecordsTest.dnsBelgiumRootRecords())
        .addRecords("www", RecordsTest.dnsBelgiumWwwRecords())
        .addRecords("_dmarc", RecordsTest.dnsBelgiumDmarcRecords());
  }
}
