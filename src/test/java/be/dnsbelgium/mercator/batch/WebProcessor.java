package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.vat.WebCrawler;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebCrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebProcessor implements ItemProcessor<VisitRequest, WebCrawlResult> {

  private final WebCrawler webCrawler;
  private static final Logger logger = LoggerFactory.getLogger(WebProcessor.class);

  @Autowired
  WebProcessor(WebCrawler webCrawler) {
    this.webCrawler = webCrawler;
  }

  WebProcessor() {
    this.webCrawler = null;
  }

  @Override
  public WebCrawlResult process(@NonNull VisitRequest request) {
    logger.info("request = {}", request);

    if (webCrawler != null) {
      List<WebCrawlResult> results = webCrawler.collectData(request);
      return results.isEmpty() ? null : results.getFirst();
    }

    return WebCrawlResult.builder()
            .visitId(request.getVisitId())
            .domainName(request.getDomainName())
            .build();

  }
}
