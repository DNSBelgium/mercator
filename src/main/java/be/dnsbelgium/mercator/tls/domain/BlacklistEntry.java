package be.dnsbelgium.mercator.tls.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
public class BlacklistEntry {

  private String cidrPrefix;

}
