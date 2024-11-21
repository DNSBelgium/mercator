package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.geoip.MaxMindConfig;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableConfigurationProperties({MaxMindConfig.class, SmtpConfig.class})
@EnableScheduling
public class SmtpCrawlerConfiguration {

  @Value("${crawler.smtp.geoIP.enabled:false}")
  boolean geoIpEnabled;


}
