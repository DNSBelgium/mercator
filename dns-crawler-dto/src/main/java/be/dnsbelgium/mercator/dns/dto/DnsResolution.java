package be.dnsbelgium.mercator.dns.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.*;

@Getter
@ToString
public class DnsResolution {
  private final String domainName;
  private final Map<String, Records> records;
  private final boolean ok;
  private final Error error;

  private DnsResolution(String domainName, Map<String, Records> records, boolean ok, Error error) {
    this.domainName = domainName;
    this.records = records;
    this.ok = ok;
    this.error = error;
  }

  public static DnsResolution nxdomain(String domainName, String prefix, Records records) {
    return failed(domainName, prefix, Error.NXDOMAIN).addRecords(prefix, records);
  }

  public static DnsResolution failed(String domainName, String prefix, Error error) {
    return new DnsResolution(domainName, new HashMap<>(), false, error);
  }

  public static DnsResolution failed(String domainName, String prefix) {
    return new DnsResolution(domainName, new HashMap<>(), false, Error.OTHER);
  }

  public static DnsResolution withRecords(String domainName, String prefix, Records allRecords) {
    return new DnsResolution(domainName, new HashMap<>(Map.of(prefix, allRecords)), true, null);
  }

  public static DnsResolution timeout(String domainName, String prefix) {
    return failed(domainName, prefix, Error.TIMEOUT);
  }

  public static DnsResolution networkError(String domainName, String prefix) {
    return failed(domainName, prefix, Error.NETWORK);
  }

  public static DnsResolution empty(String domainName, String prefix) {
    return new DnsResolution(domainName, new HashMap<>(), true, null);
  }

  public DnsResolution addRecords(String prefix, Records allRecords) {
    records.merge(prefix, allRecords, Records::merge);
    return this;
  }

  public String getHumanReadableProblem() {
    return "todo";
  }

  public Records getRecords(String prefix) {
    return records.get(prefix);
  }

  public enum Error {
    TIMEOUT,
    NETWORK,
    NXDOMAIN,
    OTHER
  }
}
