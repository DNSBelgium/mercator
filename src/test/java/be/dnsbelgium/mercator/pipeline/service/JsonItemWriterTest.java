package be.dnsbelgium.mercator.pipeline.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;


class JsonItemWriterTest {

    /** A simple structured payload so DuckDB's read_json_auto produces named columns. */
    private record Person(String name, int age) { }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    /**
     * A {@link ParquetConverter} that records every glob it is asked to convert and turns the
     * matched JSON into a Parquet file (via {@code read_json_auto}) so tests can count rows.
     */
    private static final class RecordingConverter implements ParquetConverter {
        private final JdbcClient client;
        private final Path parquetDir;
        final List<String> globs = new ArrayList<>();

        RecordingConverter(JdbcClient client, Path parquetDir) {
            this.client = client;
            this.parquetDir = parquetDir;
            try {
                Files.createDirectories(parquetDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void convert(String jsonGlob) {
            globs.add(jsonGlob);
            Path parquet = parquetDir.resolve(UUID.randomUUID() + ".parquet");
            //noinspection SqlSourceToSinkFlow
            client.sql("COPY (SELECT * FROM read_json_auto('" + jsonGlob + "')) "
                    + "TO '" + parquet.toAbsolutePath() + "' (FORMAT PARQUET)").update();
        }
    }

    /** A converter that always fails, to exercise the "leave JSON for inspection" policy. */
    private static final class FailingConverter implements ParquetConverter {
        final List<String> globs = new ArrayList<>();

        @Override
        public void convert(String jsonGlob) {
            globs.add(jsonGlob);
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void rollsUpEveryBatchSize_delegatesToConverter_andDeletesJsonFiles(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        Path parquetDir = dir.resolve("parquet");
        RecordingConverter converter = new RecordingConverter(client, parquetDir);
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 3);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));
        writer.write(new Person("c", 3));

        // The converter was invoked once, with a glob matching the first batch directory.
        assertThat(converter.globs).hasSize(1);
        assertThat(converter.globs.getFirst()).endsWith("/batch-1/*.json");
        // On success the JSON files (and their batch directory) are removed.
        assertThat(jsonFilesRecursively(dir)).isEmpty();
        assertThat(dir.resolve("batch-1")).doesNotExist();
        assertThat(parquetRowCountInDir(client, parquetDir)).isEqualTo(3);
        assertThat(writer.writtenItems()).isEqualTo(3);
    }

    @Test
    void flush_rollsUpPartialBatch(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        Path parquetDir = dir.resolve("parquet");
        RecordingConverter converter = new RecordingConverter(client, parquetDir);
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 10);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));

        // Below the batch size: nothing converted yet, JSON still buffered in the batch dir.
        assertThat(converter.globs).isEmpty();
        assertThat(jsonFilesRecursively(dir)).hasSize(2);

        writer.flush();

        assertThat(converter.globs).hasSize(1);
        assertThat(jsonFilesRecursively(dir)).isEmpty();
        assertThat(parquetRowCountInDir(client, parquetDir)).isEqualTo(2);
    }

    @Test
    void multipleBatches_areIsolated_andFlushHandlesRemainder(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        Path parquetDir = dir.resolve("parquet");
        RecordingConverter converter = new RecordingConverter(client, parquetDir);
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 2);

        for (int i = 0; i < 5; i++) {
            writer.write(new Person("p" + i, i));
        }

        // Two full batches (4 items) converted; 1 item still buffered as JSON.
        assertThat(converter.globs).hasSize(2);
        assertThat(jsonFilesRecursively(dir)).hasSize(1);

        writer.flush();

        assertThat(converter.globs).hasSize(3);
        assertThat(converter.globs).containsExactly(
                dir.resolve("batch-1").toAbsolutePath() + "/*.json",
                dir.resolve("batch-2").toAbsolutePath() + "/*.json",
                dir.resolve("batch-3").toAbsolutePath() + "/*.json");
        assertThat(jsonFilesRecursively(dir)).isEmpty();
        assertThat(writer.writtenItems()).isEqualTo(5);
        assertThat(parquetRowCountInDir(client, parquetDir)).isEqualTo(5);
    }

    @Test
    void failedRollUp_leavesJsonForInspection_andWriterContinues(@TempDir Path dir) throws IOException {
        FailingConverter converter = new FailingConverter();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 2);

        // First batch fails: its JSON files are kept for inspection under batch-1.
        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));

        assertThat(converter.globs).hasSize(1);
        assertThat(dir.resolve("batch-1")).exists();
        assertThat(jsonFilesRecursively(dir.resolve("batch-1"))).hasSize(2);

        // The writer advances to a fresh batch directory and keeps working.
        writer.write(new Person("c", 3));
        writer.write(new Person("d", 4));

        assertThat(converter.globs).hasSize(2);
        assertThat(dir.resolve("batch-2")).exists();
        assertThat(writer.writtenItems()).isEqualTo(4);
    }
}

