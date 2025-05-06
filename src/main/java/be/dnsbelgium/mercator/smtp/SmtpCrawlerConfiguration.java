package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.geoip.MaxMindConfig;
import be.dnsbelgium.mercator.smtp.domain.crawler.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableConfigurationProperties({MaxMindConfig.class, SmtpConfig.class})
@EnableScheduling
public class SmtpCrawlerConfiguration {

  @Value("${crawler.smtp.blocking:true}")
  boolean blockingSmtp;

  private static final Logger logger = LoggerFactory.getLogger(SmtpCrawlerConfiguration.class);

  @Bean
  SmtpIpAnalyzer smtpIpAnalyzer(MeterRegistry meterRegistry, SmtpConfig smtpConfig, SmtpConversationFactory conversationFactory, GeoIPService geoIPService) {
    if (blockingSmtp) {
      logger.info("SmtpCrawlerConfiguration: blockingSmtp is enabled");
      return new BlockingSmtpIpAnalyzer(meterRegistry, smtpConfig, geoIPService);
    }
    logger.info("SmtpCrawlerConfiguration: blockingSmtp is disabled");
    return new DefaultSmtpIpAnalyzer(meterRegistry, conversationFactory, geoIPService);
  }


}
