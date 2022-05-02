package be.dnsbelgium.mercator.dns.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RRecord {

  private Long ttl;
  private String data;

  public static RRecord of(Long ttl, String data) {
    return new RRecord(ttl, data);
  }

}
