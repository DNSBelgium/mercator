package be.dnsbelgium.mercator.web;

import be.dnsbelgium.mercator.web.domain.PageFetcherConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PageFetcherConfig.class})
public class VatCrawlerConfiguration {

}
