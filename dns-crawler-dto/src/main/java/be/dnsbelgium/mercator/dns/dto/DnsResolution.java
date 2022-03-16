package be.dnsbelgium.mercator.dns.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
public class DnsResolution {
  private final Map<String, Records> records;
  private final boolean ok;
  private final String humanReadableProblem;

  private DnsResolution(Map<String, Records> records, boolean ok, String humanReadableProblem) {
    this.records = records;
    this.ok = ok;
    this.humanReadableProblem = humanReadableProblem;
  }

  public static DnsResolution nxdomain() {
    return failed("nxdomain");
  }

  public static DnsResolution failed(String humanReadableReason) {
    return new DnsResolution(Collections.emptyMap(), false, humanReadableReason);
  }

  public static DnsResolution withRecords(String subdomain, Records allRecords) {
    return new DnsResolution(new HashMap<>(Map.of(subdomain, allRecords)), true, null);
  }

  public DnsResolution addRecords(String subdomain, Records allRecords) {
    records.merge(subdomain, allRecords, Records::add);
    return this;
  }

  public Records getRecords(String subdomain) {
    return records.get(subdomain);
  }
}
