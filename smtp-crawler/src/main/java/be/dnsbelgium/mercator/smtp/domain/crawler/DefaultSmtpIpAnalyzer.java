package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.InetAddress;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DefaultSmtpIpAnalyzer implements SmtpIpAnalyzer {

  private final SmtpConversationFactory conversationFactory;

  /* I could not get the @Cacheable working so using an explicit cache instead, which allows us to send metrics to micrometer */
  private final SmtpConversationCache cache;
  private final MeterRegistry meterRegistry;
  private final GeoIPService geoIPService;

  private static final Logger logger = getLogger(DefaultSmtpIpAnalyzer.class);

  @Autowired
  public DefaultSmtpIpAnalyzer(
    MeterRegistry meterRegistry,
    SmtpConversationFactory conversationFactory,
    GeoIPService geoIPService,
    SmtpConversationCache cache) {
    this.conversationFactory = conversationFactory;
    this.meterRegistry = meterRegistry;
    this.geoIPService = geoIPService;
    this.cache = cache;
  }

  @Override
  public SmtpConversation crawl(InetAddress ip) {
    SmtpConversation smtpConversation = cache.get(ip.getHostAddress());
    logger.debug("cache for {} = {}", ip, smtpConversation);
    if (smtpConversation == null) {
      meterRegistry.counter(MetricName.COUNTER_CACHE_MISSES).increment();
      smtpConversation = meterRegistry.timer(MetricName.TIMER_IP_CRAWL).record(() -> doCrawl(ip));
      if (smtpConversation != null) {
        geoIP(smtpConversation);
        meterRegistry.gauge(MetricName.GAUGE_CACHE_SIZE, cache.size());
      }
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
