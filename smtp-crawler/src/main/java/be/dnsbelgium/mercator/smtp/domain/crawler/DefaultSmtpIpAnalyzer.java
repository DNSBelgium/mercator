package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DefaultSmtpIpAnalyzer implements SmtpIpAnalyzer {

  private final SmtpConversationFactory conversationFactory;

  /* I could not get the @Cacheable working so using an explicit cache instead, which allows us to send metrics to micrometer */
  private final Cache<InetAddress, SmtpConversation> cache;
  private final MeterRegistry meterRegistry;
  private final GeoIPService geoIPService;

  private static final Logger logger = getLogger(DefaultSmtpIpAnalyzer.class);

  @Autowired
  public DefaultSmtpIpAnalyzer(
      MeterRegistry meterRegistry,
      SmtpConversationFactory conversationFactory,
      GeoIPService geoIPService,
      @Value("${smtp.crawler.ip.cache.size.initial:2000}") int initialCacheSize,
      @Value("${smtp.crawler.ip.cache.size.max:50000}") int maxCacheSize,
      @Value("${smtp.crawler.ip.cache.ttl.hours:24}") int ttlHours) {
    this.conversationFactory = conversationFactory;
    this.meterRegistry = meterRegistry;
    this.geoIPService = geoIPService;
    logger.info("initialCacheSize={} maxCacheSize={} ttl={} hours", initialCacheSize, maxCacheSize, ttlHours);
    if (maxCacheSize == 0) {
      logger.warn("maxCacheSize=0 => caching is actually disabled!!");
    }
    this.cache = Caffeine.newBuilder()
        .initialCapacity(initialCacheSize)
        .maximumSize(maxCacheSize)
        .expireAfterWrite(ttlHours, TimeUnit.HOURS)
        .recordStats()
        .build();
  }

  @Override
  public SmtpConversation crawl(InetAddress ip) {
    SmtpConversation smtpConversation = cache.getIfPresent(ip);
    logger.debug("cache for {} = {}", ip, smtpConversation);
    if (smtpConversation == null) {
      meterRegistry.counter(MetricName.COUNTER_CACHE_MISSES).increment();
      smtpConversation = meterRegistry.timer(MetricName.TIMER_IP_CRAWL).record(() -> doCrawl(ip));
      geoIP(smtpConversation);
      cache.put(ip, smtpConversation);
      meterRegistry.gauge(MetricName.GAUGE_CACHE_SIZE, cache.estimatedSize());
    } else {
      meterRegistry.counter(MetricName.COUNTER_CACHE_HITS).increment();
    }
    return smtpConversation;
  }

  private SmtpConversation doCrawl(InetAddress ip) {
    logger.debug("About to talk SMTP with {}", ip);
    ISmtpConversation conversation = conversationFactory.create(ip);
    return conversation.talk();
  }

  private void geoIP(SmtpConversation smtpConversation) {
    Optional<Pair<Integer, String>> asn = geoIPService.lookupASN(smtpConversation.getIp());
    if (asn.isPresent()) {
      smtpConversation.setAsn(Long.valueOf(asn.get().getKey()));
      smtpConversation.setAsnOrganisation(asn.get().getValue());
    }
    Optional<String> country = geoIPService.lookupCountry(smtpConversation.getIp());
    country.ifPresent(smtpConversation::setCountry);
  }

}
