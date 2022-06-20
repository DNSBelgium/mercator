package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;

public class TlsCrawlerService {




  public void crawl(VisitRequest visitRequest) {

    /*

     * resolve name => IPv4 address
     * check cache
     * if not in cache
       * check if port 443 open
       * check SSL 2.0
       * check SSL 3.0
       * check TLS 1.0
       * check TLS 1.1
       * check TLS 1.2
       * check TLS 1.3
     */

    TlsCrawlResult crawlResult = new TlsCrawlResult();

  }


  public ProtocolScanResult scan(VisitRequest visitRequest, TlsProtocolVersion protocolVersion) {
    return null;
  }

}
