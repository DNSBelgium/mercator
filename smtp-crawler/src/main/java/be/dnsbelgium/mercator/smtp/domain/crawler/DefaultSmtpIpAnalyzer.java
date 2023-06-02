package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DefaultSmtpIpAnalyzer implements SmtpIpAnalyzer {

  private final SmtpConversationFactory conversationFactory;
  private final MeterRegistry meterRegistry;
  private final GeoIPService geoIPService;
  private final JedisPool jedisPool;
  private final int ttlHours;

  private static final Logger logger = getLogger(DefaultSmtpIpAnalyzer.class);

  @Autowired
  public DefaultSmtpIpAnalyzer(
      MeterRegistry meterRegistry,
      SmtpConversationFactory conversationFactory,
      GeoIPService geoIPService,
      @Value("${smtp.crawler.ip.cache.ttl.hours:24}") int ttlHours,
      @Value("${smtp.crawler.ip.cache.host:cache}") String cacheHost,
      @Value("${smtp.crawler.ip.cache.port:6379}") int cachePort) {
    this.conversationFactory = conversationFactory;
    this.meterRegistry = meterRegistry;
    this.geoIPService = geoIPService;
    logger.info("ttl={} hours", ttlHours);
    this.jedisPool = new JedisPool(cacheHost, cachePort);
    this.ttlHours = ttlHours;
  }

  @Override
  public SmtpConversation crawl(InetAddress ip) {
    SmtpConversation smtpConversation = null;
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()).disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    try (Jedis jedis = jedisPool.getResource()){
      String jedisResponse = jedis.get(ip.getHostAddress());
      logger.debug("cache for {} = {}", ip, jedisResponse);
      if (jedisResponse == null) {
        meterRegistry.counter(MetricName.COUNTER_CACHE_MISSES).increment();
        smtpConversation = meterRegistry.timer(MetricName.TIMER_IP_CRAWL).record(() -> doCrawl(ip));
        geoIP(smtpConversation);
        try {
          String conversation = objectMapper.writeValueAsString(smtpConversation);
          jedis.set(ip.getHostAddress(), conversation);
          int ttlSeconds = ttlHours * 60 * 60;
          jedis.expire(ip.getHostAddress(), ttlSeconds);
        } catch (IOException e) {
          e.printStackTrace();
        }
        meterRegistry.gauge(MetricName.GAUGE_CACHE_SIZE, jedis.dbSize());
      } else {
        try {
          smtpConversation = objectMapper.readValue(jedisResponse, SmtpConversation.class);
        } catch (IOException e) {
          e.printStackTrace();
        }
        meterRegistry.counter(MetricName.COUNTER_CACHE_HITS).increment();
      }
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
