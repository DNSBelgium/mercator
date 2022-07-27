package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.common.messaging.ack.AckConfig;
import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.net.ssl.HostnameVerifier;
import java.security.Security;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@Import(AckConfig.class)
public class TlsCrawlerConfiguration implements JmsConfig {

  private static final Logger logger = getLogger(TlsCrawlerConfiguration.class);

  @Bean
  HostnameVerifier hostnameVerifier() {
    return new DefaultHostnameVerifier();
  }

}
