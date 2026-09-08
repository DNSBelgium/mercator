package be.dnsbelgium.mercator.pipeline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Jackson 3 ({@code tools.jackson}) configuration for the pipeline.
 *
 * <p>Spring Boot 4 auto-configures a {@code @Primary} {@link JsonMapper} that the web/REST
 * layer uses. The pipeline, however, must serialize crawl results to JSON <em>exactly</em>
 * like the legacy Spring Batch writer did, because the DuckDB {@code read_json(columns={...})}
 * schemas in {@code sql/**\/cte_definitions.sql} expect:
 * <ul>
 *   <li><strong>snake_case</strong> property names ({@code visit_id}, {@code crawl_started}, …), and</li>
 *   <li>timestamps formatted as {@code yyyy-MM-dd HH:mm:ss.SSSSSS} (UTC, always 6 fraction digits).</li>
 * </ul>
 *
 * <p>This is the Jackson 3 twin of the legacy Jackson 2 mapper in
 * {@code be.dnsbelgium.mercator.batch.JsonConfiguration}. To avoid changing the global
 * (web) mapper, it is exposed as a <strong>dedicated, non-{@code @Primary}</strong> bean
 * named {@link #PIPELINE_OBJECT_MAPPER}. Its return type is the abstract {@link ObjectMapper}
 * (not {@link JsonMapper}) so Boot's {@code @ConditionalOnMissingBean(JsonMapper)} auto-config
 * is <em>not</em> disabled and the primary web mapper keeps working. Pipeline components
 * select this mapper via {@code @Qualifier(PipelineJacksonConfig.PIPELINE_OBJECT_MAPPER)}.
 */
@Configuration
public class PipelineJacksonConfig {

    /** Bean name / qualifier for the pipeline-specific Jackson 3 mapper. */
    public static final String PIPELINE_OBJECT_MAPPER = "pipelineObjectMapper";

    /**
     * Serializes an {@link Instant} as {@code yyyy-MM-dd HH:mm:ss.SSSSSS} at UTC, always
     * emitting 6 fraction digits (works around a DuckDB quirk). Mirrors the legacy
     * {@code JsonConfiguration.CustomInstantSerializer}.
     */
    public static class InstantToStringSerializer extends ValueSerializer<Instant> {
        static final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(FORMATTER.format(value));
        }
    }

    /**
     * Parses an {@link Instant} from {@code yyyy-MM-dd HH:mm:ss} with an optional fraction
     * (0–9 digits), so values written by DuckDB (which does not always add a fraction) round-trip.
     * Mirrors the legacy {@code JsonConfiguration.CustomInstantDeserializer}.
     */
    public static class StringToInstantDeserializer extends ValueDeserializer<Instant> {
        static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .toFormatter()
                .withZone(ZoneOffset.UTC);

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) {
            return Instant.from(FORMATTER.parse(p.getString()));
        }
    }

    /**
     * The pipeline's Jackson 3 mapper: snake_case property names plus the custom
     * {@link Instant} (de)serializers. Declared as {@link ObjectMapper} on purpose (see the
     * class-level Javadoc) and intentionally not {@code @Primary}.
     */
    @Bean(PIPELINE_OBJECT_MAPPER)
    public ObjectMapper pipelineObjectMapper() {
        SimpleModule instantModule = new SimpleModule()
                .addSerializer(Instant.class, new InstantToStringSerializer())
                .addDeserializer(Instant.class, new StringToInstantDeserializer());
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .addModule(instantModule)
                .build();
    }
}

