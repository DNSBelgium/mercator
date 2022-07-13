package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
public class BlacklistEntry {

  @Id
  private String cidrPrefix;

}
