package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.vat.domain.PageFetcherConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PageFetcherConfig.class})
public class VatCrawlerConfiguration {

}
