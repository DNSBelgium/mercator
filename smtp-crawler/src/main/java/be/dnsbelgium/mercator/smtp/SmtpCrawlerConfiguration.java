package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.messaging.ack.AckConfig;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import be.dnsbelgium.mercator.geoip.DisabledGeoIPService;
import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.geoip.GeoIPServiceImpl;
import be.dnsbelgium.mercator.geoip.MaxMindConfig;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableConfigurationProperties({MaxMindConfig.class, SmtpConfig.class})
@Import(AckConfig.class)
@EnableScheduling
public class SmtpCrawlerConfiguration implements JmsConfig {

  @Value("${crawler.smtp.geoIP.enabled}")
  boolean geoIpEnabled;

  private static final Logger logger = getLogger(SmtpCrawlerConfiguration.class);

  @Bean
  public GeoIPService geoIPService(MaxMindConfig maxMindConfig) {
    if (geoIpEnabled) {
      return new GeoIPServiceImpl(maxMindConfig);
    } else {
      logger.info("Geo IP is disabled => using a {}", DisabledGeoIPService.class);
      return new DisabledGeoIPService();
    }
  }

}
