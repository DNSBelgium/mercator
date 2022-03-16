package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import be.dnsbelgium.mercator.ssl.crawler.persistence.CipherSuiteSupportCompositeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@IdClass(CipherSuiteSupportCompositeId.class)
@Table(name = "cipher_suite_support")
public class CipherSuiteSupport {

  @Id
  @Column(name = "ssl_crawl_result_id")
  private Long sslCrawlResultId;

  @Id
  @Column(name = "cipher_suite_id")
  private String cipherSuiteId;

  @Column(name = "protocol")
  private String protocol;

  @Column(name = "supported")
  private Boolean supported;

}
