package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class CountCipherSuitesResult implements Serializable {
  @Id
  @Column(name = "protocol")
  private String protocol;
  @Column(name = "count")
  private Integer count;

}
