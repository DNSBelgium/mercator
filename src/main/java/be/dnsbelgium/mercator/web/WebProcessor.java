package be.dnsbelgium.mercator.web;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebProcessor implements ItemProcessor<VisitRequest, WebCrawlResult> {

  private final WebCrawler webCrawler;
  private static final Logger logger = LoggerFactory.getLogger(WebProcessor.class);

  @Autowired
  public WebProcessor(WebCrawler webCrawler) {
    this.webCrawler = webCrawler;
  }

  @Override
  public WebCrawlResult process(@NonNull VisitRequest request) {
    try {
      logger.info("request = {}", request);
      return webCrawler.crawl(request);
    } catch (Exception e) {
      logger.error("failed to crawl {}", request, e);
      return null;
    }
  }

  @Override
  public WebCrawlResult processItem(VisitRequest item) {
    return process(item);
  }

}
