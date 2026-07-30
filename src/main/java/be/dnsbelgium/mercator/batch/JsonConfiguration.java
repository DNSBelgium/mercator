package be.dnsbelgium.mercator.batch;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@Configuration
public class JsonConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(JsonConfiguration.class);


  public static class CustomInstantSerializer extends ValueSerializer<Instant> {
    // Always serialize with 6 digits (duckdb bug?)
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext serializers) {
      gen.writeString(DATE_TIME_FORMATTER.format(value));
    }
  }

  public static class CustomInstantDeserializer extends ValueDeserializer<Instant> {
    // always deserialize with flexible fraction because duckdb does not always add a fraction
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .toFormatter()
        .withZone(ZoneOffset.UTC);
    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) {
      return Instant.from(DATE_TIME_FORMATTER.parse(p.getString()));
    }
  }

  @Bean
  @Primary
  public JsonMapper mapper() {
    logger.debug("Creating a JsonMapper");
    return JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .constructorDetector(ConstructorDetector.DEFAULT.withAllowImplicitWithDefaultConstructor(false))
        .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
        .addModule(new SimpleModule().addSerializer(Instant.class, new CustomInstantSerializer()).addDeserializer(Instant.class, new CustomInstantDeserializer()))
        .build();
  }



}
