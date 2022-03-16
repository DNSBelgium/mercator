package be.dnsbelgium.mercator.ssl.crawler.persistence;

import java.io.Serializable;

public class CipherSuiteSupportCompositeId implements Serializable {
  private Long sslCrawlResultId;
  private String cipherSuiteId;
}
