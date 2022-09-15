package be.dnsbelgium.mercator.dns.dto;

public record RRecord(long ttl, String data) {

  public static RRecord of(long ttl, String data) {
    return new RRecord(ttl, data);
  }

}
