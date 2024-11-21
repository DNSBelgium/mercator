package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.dns.domain.geoip.DisabledGeoIpEnricher;
import be.dnsbelgium.mercator.dns.domain.geoip.EnabledGeoIpEnricher;
import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.geoip.DisabledGeoIPService;
import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.geoip.GeoIPServiceImpl;
import be.dnsbelgium.mercator.geoip.MaxMindConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableConfigurationProperties({MaxMindConfig.class})
public class DnsCrawlerConfiguration  {

    @Value("${crawler.dns.geoIP.enabled:false}")
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

    @Bean
    public GeoIpEnricher geoIpEnricher(GeoIPService geoIPService) {
        if (geoIpEnabled) {
            return new EnabledGeoIpEnricher(geoIPService);
        } else {
            return new DisabledGeoIpEnricher();
        }
    }

}
