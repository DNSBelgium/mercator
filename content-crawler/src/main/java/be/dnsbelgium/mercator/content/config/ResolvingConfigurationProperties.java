package be.dnsbelgium.mercator.content.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "content.resolving")
public class ResolvingConfigurationProperties {

  private final Map<String, String> requestQueues;
  private final Map<String, String> responseQueues;

}
