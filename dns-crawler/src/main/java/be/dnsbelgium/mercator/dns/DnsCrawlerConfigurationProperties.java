package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "crawler.dns")
public class DnsCrawlerConfigurationProperties {

  private final Map<String, List<RecordType>> subdomains;

}
