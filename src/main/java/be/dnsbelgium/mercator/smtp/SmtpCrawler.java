package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpAnalyzer;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConversationCache;
import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class SmtpCrawler implements ItemProcessor<VisitRequest, SmtpVisit> {

  private final SmtpAnalyzer smtpAnalyzer;
  private final SmtpConversationCache cache;

  private static final Logger logger = LoggerFactory.getLogger(SmtpCrawler.class);

  public SmtpCrawler(SmtpAnalyzer smtpAnalyzer, SmtpConversationCache cache) {
    this.smtpAnalyzer = smtpAnalyzer;
    this.cache = cache;
  }

  @Scheduled(fixedRate = 15, initialDelay = 15, timeUnit = TimeUnit.MINUTES)
  public void clearCacheScheduled() {
    logger.info("clearCacheScheduled: evicting entries older than 4 hours");
    cache.evictEntriesOlderThan(Duration.ofHours(24));
  }

  @Override
  public SmtpVisit process(VisitRequest visitRequest) {
    SmtpVisit smtpVisit = smtpAnalyzer.visit(visitRequest.getDomainName());
    smtpVisit.setVisitId(visitRequest.getVisitId());
    addToCache(smtpVisit);
    return smtpVisit;
  }

  @SuppressWarnings("unused")
  private void addToCache(SmtpVisit visit) {
    for (SmtpHost host : visit.getHosts()) {
      var conversation = host.getConversation();
      logger.debug("Saving conversation with {} in the cache", conversation.getIp());
      cache.add(conversation.getIp(), conversation);
    }
  }
}

