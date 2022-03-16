package be.dnsbelgium.mercator.content.ports;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.work.Crawler;
import be.dnsbelgium.mercator.content.domain.ContentCrawlService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ContentCrawler implements Crawler {
  private final ContentCrawlService service;

  public ContentCrawler(ContentCrawlService service) {
    this.service = service;
  }

  @JmsListener(destination = "${content.crawler.input.queue.name}")
  public void process(VisitRequest item) {
    service.retrieveContent(item);
  }

}
