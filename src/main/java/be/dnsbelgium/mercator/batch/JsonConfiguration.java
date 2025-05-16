package be.dnsbelgium.mercator.batch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@Configuration
public class JsonConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(JsonConfiguration.class);

  private static DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd HH:mm:ss")
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .optionalEnd()
      .toFormatter()
      .withZone(ZoneOffset.UTC);

  public static class CustomInstantSerializer extends JsonSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeString(DATE_TIME_FORMATTER.format(value));
    }
  }

  public class CustomInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return Instant.from(DATE_TIME_FORMATTER.parse(p.getText()));
    }
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .registerModule(new SimpleModule().addSerializer(Instant.class, new CustomInstantSerializer()).addDeserializer(Instant.class, new CustomInstantDeserializer()))
        .registerModule(new Jdk8Module())
        .registerModule(new JsonMixinModule())
        .registerModule(new ParameterNamesModule())
        .registerModule(new JsonComponentModule());
    logger.info("objectMapper.getPropertyNamingStrategy = {}", objectMapper.getPropertyNamingStrategy());
    return objectMapper;
  }
}
