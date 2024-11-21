package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SmtpConversationCache {
  private static final Logger logger = getLogger(SmtpConversationCache.class);

  private final ConcurrentHashMap<String, SmtpConversation> cache = new ConcurrentHashMap<>();

  @Autowired
  public SmtpConversationCache(MeterRegistry meterRegistry) {
    meterRegistry.gaugeMapSize(MetricName.GAUGE_CACHE_SIZE, Tags.empty(), cache);
  }

  public void add(String ip, SmtpConversation conversation){
    cache.put(ip, conversation);
  }

  public SmtpConversation get(String ip){
    return cache.get(ip);
  }

  public int size(){
    return cache.size();
  }

  public void evictEntriesOlderThan(Duration duration) {
    Instant notBefore = Instant.now().minus(duration);
    logger.info("Evicting entries that older than {}, so after {}", duration, notBefore);
    int entriesBefore = cache.size();
    cache.entrySet().removeIf(e -> e.getValue().getTimestamp().isBefore(notBefore));
    int entriesAfter = cache.size();
    logger.info("Eviction done. before cache had {} entries, now it has {} entries", entriesBefore, entriesAfter);
  }
}
