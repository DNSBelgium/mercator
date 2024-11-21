package be.dnsbelgium.mercator.tls;

import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HostnameVerifier;

@Configuration
public class TlsCrawlerConfiguration {

  @Bean
  HostnameVerifier hostnameVerifier() {
    return new DefaultHostnameVerifier();
  }

}
