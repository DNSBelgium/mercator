package be.dnsbelgium.mercator.content.domain;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.content.domain.content.ContentResolver;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import be.dnsbelgium.mercator.content.dto.WappalyzerResolution;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResult;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContentCrawlService {

  private static final Logger logger = LoggerFactory.getLogger(ContentCrawlService.class);

  private final ContentCrawlResultRepository muppetsRepo;
  private final WappalyzerResultRepository wappalyzerRepo;
  private final ContentResolver resolver;
  private final List<String> urlsToCrawl;
  private final AckMessageService ackMessageService;

  public ContentCrawlService(ContentCrawlResultRepository muppetsRepo, WappalyzerResultRepository wappalyzerRepo,
                             ContentResolver resolver, @Value("${content.crawler.url.prefixes}") List<String> urlsToCrawl,
                             AckMessageService ackMessageService) {
    this.muppetsRepo = muppetsRepo;
    this.wappalyzerRepo = wappalyzerRepo;
    this.resolver = resolver;
    this.urlsToCrawl = urlsToCrawl;
    this.ackMessageService = ackMessageService;
  }

  public void retrieveContent(VisitRequest request) {
    String domainName = request.getDomainName();

    List<String> urls = urlsToCrawl.stream().map(prefix -> prefix + domainName).collect(Collectors.toList());

    resolver.requestContentResolving(request, urls);
    logger.info("Requested content resolution for {}", domainName);
  }

  public void contentRetrieved(MuppetsResolution resolution) {
    logger.debug("Content retrieved for {}", resolution.getUrl());
    var result = ContentCrawlResult.of(resolution);
    boolean duplicate = muppetsRepo.saveAndIgnoreDuplicateKeys(result);
    if (duplicate) {
      logger.info("Content was already saved for {}", resolution.getUrl());
    } else {
      logger.debug("Content saved for {}", resolution.getUrl());
    }
    if (muppetsRepo.countByVisitId(result.getVisitId()) == urlsToCrawl.size()) {
      ackMessageService.sendAck(result.getVisitId(), result.getDomainName(), CrawlerModule.MUPPETS);
    }
  }

  public void contentRetrieved(WappalyzerResolution resolution) {
    logger.debug("Content retrieved for {}", resolution.getUrl());
    var result = WappalyzerResult.of(resolution);
    wappalyzerRepo.save(result);
    logger.debug("Content saved for {}", resolution.getUrl());
    if (wappalyzerRepo.countByVisitId(result.getVisitId()) == urlsToCrawl.size()) {
      ackMessageService.sendAck(result.getVisitId(), result.getDomainName(), CrawlerModule.WAPPALYZER);
    }
  }

}
