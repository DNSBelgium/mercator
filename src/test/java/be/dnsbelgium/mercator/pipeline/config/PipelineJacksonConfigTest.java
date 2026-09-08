package be.dnsbelgium.mercator.pipeline.config;

import be.dnsbelgium.mercator.batch.JsonConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 0 guard for the JSON→Parquet migration (see {@code agent-tasks/t2-flush-plan.MD}).
 *
 * <p>Proves that the Jackson 3 pipeline mapper produced by {@link PipelineJacksonConfig}
 * serializes objects <em>identically</em> to the legacy Jackson 2 mapper from
 * {@link JsonConfiguration} (the one the old Spring Batch writer used). This parity is what
 * lets the new writer delegate to the module {@code storeResults()} methods, whose typed
 * {@code read_json(columns={...})} schemas rely on snake_case field names and the
 * {@code yyyy-MM-dd HH:mm:ss.SSSSSS} timestamp format.
 */
class PipelineJacksonConfigTest {

    /** A payload exercising multi-word (snake_case) fields, Instants, nesting, lists and nulls. */
    record Sample(String visitId,
                  String domainName,
                  Instant crawlStarted,
                  Instant crawlFinished,
                  List<PageVisit> pageVisits,
                  String problem) { }

    record PageVisit(String finalUrl, int statusCode, Instant crawlStarted) { }

    private final com.fasterxml.jackson.databind.ObjectMapper legacyMapper = new JsonConfiguration().mapper();
    private final tools.jackson.databind.ObjectMapper pipelineMapper = new PipelineJacksonConfig().pipelineObjectMapper();
    /** Neutral tree reader: {@code readTree} copies tokens verbatim, so naming strategy is irrelevant. */
    private final tools.jackson.databind.ObjectMapper neutral = tools.jackson.databind.json.JsonMapper.builder().build();

    private static final Instant WITH_MICROS = Instant.parse("2026-09-09T12:34:56.123456Z");
    private static final Instant WITHOUT_FRACTION = Instant.parse("2026-01-02T03:04:05Z");

    private Sample sample() {
        return new Sample(
                "v-123",
                "dnsbelgium.be",
                WITH_MICROS,
                WITHOUT_FRACTION,
                List.of(new PageVisit("https://dnsbelgium.be/", 200, WITH_MICROS)),
                null);
    }

    @Test
    void pipelineMapper_producesSameJsonAsLegacyMapper() throws Exception {
        Sample sample = sample();

        String legacyJson = legacyMapper.writeValueAsString(sample);
        String pipelineJson = pipelineMapper.writeValueAsString(sample);

        // Compare as trees so field ordering differences between Jackson 2 and 3 don't matter.
        tools.jackson.databind.JsonNode legacyTree = neutral.readTree(legacyJson);
        tools.jackson.databind.JsonNode pipelineTree = neutral.readTree(pipelineJson);

        assertThat(pipelineTree)
                .as("pipeline (Jackson 3) JSON must match legacy (Jackson 2) JSON exactly")
                .isEqualTo(legacyTree);
    }

    @Test
    void pipelineMapper_usesSnakeCaseFieldNames() {
        String json = pipelineMapper.writeValueAsString(sample());

        assertThat(json)
                .contains("\"visit_id\"")
                .contains("\"domain_name\"")
                .contains("\"crawl_started\"")
                .contains("\"crawl_finished\"")
                .contains("\"page_visits\"")
                .contains("\"final_url\"")
                .contains("\"status_code\"")
                // and NOT the camelCase originals
                .doesNotContain("\"visitId\"")
                .doesNotContain("\"domainName\"")
                .doesNotContain("\"finalUrl\"");
    }

    @Test
    void pipelineMapper_formatsInstantsWithSixFractionDigits() {
        String json = pipelineMapper.writeValueAsString(sample());

        // Always six fraction digits, space (not 'T') separator, no zone suffix.
        assertThat(json).contains("\"2026-09-09 12:34:56.123456\"");
        assertThat(json).contains("\"2026-01-02 03:04:05.000000\"");
        // No ISO-8601 leakage.
        assertThat(json).doesNotContain("2026-09-09T12:34:56");
    }

    @Test
    void pipelineMapper_roundTripsInstants() {
        Sample original = sample();
        String json = pipelineMapper.writeValueAsString(original);

        Sample back = pipelineMapper.readValue(json, Sample.class);

        assertThat(back.crawlStarted()).isEqualTo(WITH_MICROS);
        assertThat(back.crawlFinished()).isEqualTo(WITHOUT_FRACTION);
        assertThat(back.pageVisits().getFirst().crawlStarted()).isEqualTo(WITH_MICROS);
    }
}

