package be.dnsbelgium.mercator.smtp.domain.crawler.config;

import be.dnsbelgium.mercator.geoip.MaxMindConfig;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@EnableConfigurationProperties({SmtpConfig.class, MaxMindConfig.class})
@SpringBootConfiguration
public class TestContext {
}
