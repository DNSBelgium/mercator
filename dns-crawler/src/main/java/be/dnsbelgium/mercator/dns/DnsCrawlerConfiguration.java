package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.common.messaging.ack.AckConfig;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import be.dnsbelgium.mercator.geoip.DisabledGeoIPService;
import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.geoip.GeoIPServiceImpl;
import be.dnsbelgium.mercator.geoip.MaxMindConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableConfigurationProperties({MaxMindConfig.class})
@Import(AckConfig.class)
public class DnsCrawlerConfiguration implements JmsConfig {

    @Value("${crawler.dns.geoIP.enabled}")
    boolean geoIpEnabled;

    private static final Logger logger = getLogger(DnsCrawlerConfiguration.class);

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
