package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpAnalyzer;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConversationCache;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import be.dnsbelgium.mercator.visits.CrawlerModule;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class SmtpCrawler implements CrawlerModule<SmtpVisit>, ItemProcessor<VisitRequest, SmtpVisit> {


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
  @Timed("smtp.collectData")
  public List<SmtpVisit> collectData(VisitRequest visitRequest) {
    SmtpVisit smtpVisit = smtpAnalyzer.visit(visitRequest.getDomainName());
    smtpVisit.setVisitId(visitRequest.getVisitId());
    return List.of(smtpVisit);
  }

  @Override
  public SmtpVisit process(VisitRequest visitRequest) {
    SmtpVisit smtpVisit = smtpAnalyzer.visit(visitRequest.getDomainName());
    smtpVisit.setVisitId(visitRequest.getVisitId());
    return smtpVisit;
  }

  @Override
  public void saveItem(SmtpVisit visit) {
    //smtpRepository.saveVisit(visit);
  }

  public void save(Object item) {
    if (item instanceof SmtpVisit visit) {
      saveItem(visit);
    } else {
      logger.error("Cannot save item of type: {}", item.getClass().getName());
    }
  }

  @Override
  public void save(List<?> collectedData) {
    collectedData.forEach(this::save);
  }


  @Override
  public void afterSave(List<?> collectedData) {
    for (Object item : collectedData) {
      if (item instanceof SmtpVisit visit) {
        addToCache(visit);
      }
    }
  }

  private void addToCache(SmtpVisit visit) {
    for (SmtpHost host : visit.getHosts()) {
      var conversation = host.getConversation();
      if (conversation.getId() == null) {
        logger.error("conversation with {} has no Id => will not save it in the cache", conversation.getIp());
      } else {
        logger.info("Saving conversation with {} in the cache, id={}", conversation.getIp(), conversation.getId());
        cache.add(conversation.getIp(), conversation);
      }
    }
  }

  @Override
  public List<SmtpVisit> find(String visitId) {
    // we will never return more than one SmtpVisit
    //return smtpRepository.findVisit(visitId).stream().toList();
    return null;
  }

  @Override
  public void createTables() {
    //smtpRepository.createTables();
  }
}
