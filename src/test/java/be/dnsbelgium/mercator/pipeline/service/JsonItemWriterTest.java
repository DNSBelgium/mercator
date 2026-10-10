package be.dnsbelgium.mercator.pipeline.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class JsonItemWriterTest {

    /** A simple structured payload so DuckDB's read_json_auto produces named columns. */
    private record Person(String name, int age) { }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    /**
     * A {@link ParquetConverter} that records every glob it is asked to convert and delegates to the
     * shared {@link be.dnsbelgium.mercator.pipeline.testsupport.TestSupport#jsonToParquetConverter}
     * so tests can count the resulting Parquet rows.
     */
    private static final class RecordingConverter implements ParquetConverter {
        private final ParquetConverter delegate;
        final List<String> globs = new ArrayList<>();

        RecordingConverter(JdbcClient client, Path parquetDir) {
            this.delegate = jsonToParquetConverter(client, parquetDir);
        }

        @Override
        public void convert(String jsonGlob) {
            globs.add(jsonGlob);
            delegate.convert(jsonGlob);
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
    void failedRollUp_leavesJsonForInspection_andFailsOnFlush(@TempDir Path dir) throws IOException {
        FailingConverter converter = new FailingConverter();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 2);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));

        assertThat(converter.globs).hasSize(1);
        assertThat(dir.resolve("batch-1")).exists();
        assertThat(jsonFilesRecursively(dir.resolve("batch-1"))).hasSize(2);

        writer.write(new Person("c", 3));
        assertThatThrownBy(writer::flush)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be converted to Parquet")
                .hasRootCauseMessage("boom");

        assertThat(converter.globs).hasSize(2);
        assertThat(dir.resolve("batch-2")).exists();
        assertThat(jsonFilesRecursively(dir.resolve("batch-2"))).hasSize(1);
        assertThat(writer.writtenItems()).isEqualTo(3);
    }

    @Test
    void commitListener_receivesIdsOfEachBatch_afterConversion(@TempDir Path dir) {
        JdbcClient client = duckDbClient();
        RecordingConverter delegate = new RecordingConverter(client, dir.resolve("parquet"));
        // One shared event log proves the ordering: a batch is converted BEFORE it is reported.
        List<String> events = new ArrayList<>();
        ParquetConverter converter = glob -> {
            events.add("convert");
            delegate.convert(glob);
        };
        BatchCommitListener listener = ids -> events.add("commit " + ids);
        JsonItemWriter<Person> writer =
                new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 2, Person::name, listener);

        for (int i = 0; i < 5; i++) {
            writer.write(new Person("p" + i, i));
        }
        writer.flush();

        assertThat(events).containsExactly(
                "convert", "commit [p0, p1]",
                "convert", "commit [p2, p3]",
                "convert", "commit [p4]");
    }

    @Test
    void commitListener_notCalledWhenRollUpFails(@TempDir Path dir) throws IOException {
        FailingConverter converter = new FailingConverter();
        List<List<String>> committed = new ArrayList<>();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(
                objectMapper, converter, dir, Person.class, 2, Person::name, committed::add);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));
        writer.write(new Person("c", 3));

        assertThatThrownBy(writer::flush)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be converted to Parquet")
                .hasRootCauseMessage("boom");

        // The rows must stay open: nothing was reported, and the JSON is still there for inspection.
        assertThat(committed).isEmpty();
        assertThat(jsonFilesRecursively(dir)).hasSize(3);
    }

    @Test
    void commitListener_failure_doesNotAffectParquetOrJsonCleanup(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        Path parquetDir = dir.resolve("parquet");
        RecordingConverter converter = new RecordingConverter(client, parquetDir);
        List<List<String>> received = new ArrayList<>();
        BatchCommitListener listener = ids -> {
            received.add(ids);
            if (received.size() == 1) {
                throw new IllegalStateException("ack failed");
            }
        };
        JsonItemWriter<Person> writer =
                new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 2, Person::name, listener);

        // A failing listener must not make write() throw nor stop later batches from being reported.
        for (int i = 0; i < 4; i++) {
            writer.write(new Person("p" + i, i));
        }

        assertThat(received).containsExactly(List.of("p0", "p1"), List.of("p2", "p3"));
        assertThat(parquetRowCountInDir(client, parquetDir)).isEqualTo(4);
        assertThat(jsonFilesRecursively(dir)).isEmpty();
        assertThatThrownBy(writer::flush)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be marked done")
                .hasRootCauseMessage("ack failed");
    }

    @Test
    void bothFailures_rollUpFailureIsThrown_andCommitFailureIsSuppressed(@TempDir Path dir) {
        JdbcClient client = duckDbClient();
        RecordingConverter delegate = new RecordingConverter(client, dir.resolve("parquet"));
        AtomicInteger conversions = new AtomicInteger();
        ParquetConverter converter = glob -> {
            if (conversions.incrementAndGet() == 1) {
                throw new IllegalStateException("boom");
            }
            delegate.convert(glob);
        };
        BatchCommitListener listener = ids -> {
            throw new IllegalStateException("ack failed");
        };
        JsonItemWriter<Person> writer =
                new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 2, Person::name, listener);

        for (int i = 0; i < 4; i++) {
            writer.write(new Person("p" + i, i));
        }

        assertThatThrownBy(writer::flush)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be converted to Parquet")
                .hasRootCauseMessage("boom")
                .hasSuppressedException(new IllegalStateException("ack failed"));
    }

    @Test
    void nullIds_areSkipped(@TempDir Path dir) {
        JdbcClient client = duckDbClient();
        RecordingConverter converter = new RecordingConverter(client, dir.resolve("parquet"));
        List<List<String>> committed = new ArrayList<>();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, converter, dir, Person.class, 3,
                person -> person.age() == 2 ? null : person.name(), committed::add);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));
        writer.write(new Person("c", 3));

        assertThat(committed).containsExactly(List.of("a", "c"));
        assertThat(writer.writtenItems()).isEqualTo(3);
    }

    @Test
    void idExtractorAndCommitListener_mustBeSetTogether(@TempDir Path dir) {
        assertThatThrownBy(() -> new JsonItemWriter<>(
                objectMapper, new FailingConverter(), dir, Person.class, 2, Person::name, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JsonItemWriter<>(
                objectMapper, new FailingConverter(), dir, Person.class, 2, null, ids -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
