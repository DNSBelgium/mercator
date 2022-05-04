package be.dnsbelgium.mercator.dns.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
public class DnsResolution {
  private final Map<String, Records> records;
  private final boolean ok;
  private final int rcode;
  private final String humanReadableProblem;

  private DnsResolution(Map<String, Records> records, boolean ok, int rcode, String humanReadableProblem) {
    this.records = records;
    this.ok = ok;
    this.rcode = rcode;
    this.humanReadableProblem = humanReadableProblem;
  }

  public static DnsResolution nxdomain() {
    return failed(3, "nxdomain");
  }

  public static DnsResolution failed(int rcode, String humanReadableReason) {
    return new DnsResolution(new HashMap<>(), false, rcode, humanReadableReason);
  }

  public static DnsResolution withRecords(String prefix, Records allRecords) {
    return new DnsResolution(new HashMap<>(Map.of(prefix, allRecords)), true, 0, null);
  }

  public DnsResolution addRecords(String prefix, Records allRecords) {
    records.merge(prefix, allRecords, Records::add);
    return this;
  }

  public Records getRecords(String prefix) {
    return records.get(prefix);
  }
}
