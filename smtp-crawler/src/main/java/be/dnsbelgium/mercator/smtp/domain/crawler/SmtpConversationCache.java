package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SmtpConversationCache {
  private static final Logger logger = getLogger(SmtpConversationCache.class);

  private final ConcurrentHashMap<String, SmtpConversation> cache = new ConcurrentHashMap<>();

  public SmtpConversationCache(){}

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
    ZonedDateTime notBefore = ZonedDateTime.now().minus(duration);
    logger.info("Evicting entries that older than {}, so after {}", duration, notBefore);
    int entriesBefore = cache.size();
    cache.entrySet().removeIf(e -> e.getValue().getTimestamp().isBefore(ChronoZonedDateTime.from(notBefore)));
    int entriesAfter = cache.size();
    logger.info("Eviction done. before cache had {} entries, now it has {} entries", entriesBefore, entriesAfter);
  }
}
