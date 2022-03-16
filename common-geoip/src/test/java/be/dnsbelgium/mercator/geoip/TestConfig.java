package be.dnsbelgium.mercator.geoip;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({MaxMindConfig.class})
@SpringBootConfiguration
public class TestConfig {

  @Bean
  GeoIPService geoIPService(MaxMindConfig config) {
    return new GeoIPServiceImpl(config);
  }

}
