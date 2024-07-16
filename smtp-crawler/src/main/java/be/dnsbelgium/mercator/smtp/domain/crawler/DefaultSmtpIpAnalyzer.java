package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.TxLogger;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DefaultSmtpIpAnalyzer implements SmtpIpAnalyzer {

  private final SmtpConversationFactory conversationFactory;

  private final MeterRegistry meterRegistry;
  private final GeoIPService geoIPService;

  private static final Logger logger = getLogger(DefaultSmtpIpAnalyzer.class);

  @Autowired
  public DefaultSmtpIpAnalyzer(
    MeterRegistry meterRegistry,
    SmtpConversationFactory conversationFactory,
    GeoIPService geoIPService) {
    this.conversationFactory = conversationFactory;
    this.meterRegistry = meterRegistry;
    this.geoIPService = geoIPService;
  }

  @Override
  public SmtpConversation crawl(InetAddress ip) {
    TxLogger.log(getClass(), "crawl");
    SmtpConversation smtpConversation = meterRegistry.timer(MetricName.TIMER_IP_CRAWL).record(() -> doCrawl(ip));
    if (smtpConversation != null) {
        geoIP(smtpConversation);
    }
    return smtpConversation;
  }

  private SmtpConversation doCrawl(InetAddress ip) {
    TxLogger.log(getClass(), "doCrawl");
    logger.debug("About to talk SMTP with {}", ip);
    Conversation conversation = conversationFactory.create(ip);
    return conversation.talk();
  }

  private void geoIP(SmtpConversation smtpConversation) {
    Optional<Pair<Long, String>> asn = geoIPService.lookupASN(smtpConversation.getIp());
    if (asn.isPresent()) {
      smtpConversation.setAsn(asn.get().getKey());
      smtpConversation.setAsnOrganisation(asn.get().getValue());
    }
    Optional<String> country = geoIPService.lookupCountry(smtpConversation.getIp());
    country.ifPresent(smtpConversation::setCountry);
  }

}
