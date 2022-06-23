package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.common.messaging.ack.AckConfig;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AckConfig.class)
public class TlsCrawlerConfiguration implements JmsConfig {

}
