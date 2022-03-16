package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.messaging.ack.AckConfig;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import be.dnsbelgium.mercator.vat.domain.PageFetcherConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AckConfig.class)
@EnableConfigurationProperties({PageFetcherConfig.class})
public class VatCrawlerConfiguration implements JmsConfig {

}
