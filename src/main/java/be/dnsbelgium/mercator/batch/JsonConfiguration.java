package be.dnsbelgium.mercator.batch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

@Configuration
public class JsonConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(JsonConfiguration.class);

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonMixinModule())
            .registerModule(new ParameterNamesModule())
            .registerModule(new JsonComponentModule());
    objectMapper.configOverride(Instant.class)
        .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSX"));
    logger.info("objectMapper.getPropertyNamingStrategy = {}", objectMapper.getPropertyNamingStrategy());
    return objectMapper;
  }
}
