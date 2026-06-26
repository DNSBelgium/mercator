package be.dnsbelgium.mercator.web;

import be.dnsbelgium.mercator.web.domain.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PageFetcherConfig.class})
@Slf4j
public class WebCrawlerConfiguration {


    @Bean
    @ConditionalOnProperty(name = "use.vat.scraper", havingValue = "true", matchIfMissing = true)
    VatScraper vatScraper(MeterRegistry meterRegistry, PageFetcher pageFetcher, VatFinder vatFinder, VatLinkPrioritizer linkPrioritizer) {
        log.info("use.vat.scraper=true => Using VatScraper");
        return new VatScraper(meterRegistry, pageFetcher, vatFinder, linkPrioritizer);
    }

    @Bean
    @ConditionalOnProperty(name = "use.vat.scraper", havingValue = "false")
    SiteScraper siteScraper(MeterRegistry meterRegistry, PageFetcher pageFetcher, ExternalLinkPrioritizer linkPrioritizer) {
        log.info("use.vat.scraper=false => Using SiteScraper");
        return new SiteScraper(meterRegistry, pageFetcher, linkPrioritizer);
    }

}
