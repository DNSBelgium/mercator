package be.dnsbelgium.mercator.pipeline.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;


class JsonItemWriterTest {

    /** A simple structured payload so DuckDB's read_json_auto produces named columns. */
    private record Person(String name, int age) { }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void rollsUpEveryBatchSize_andDeletesJsonFiles(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, client, dir, Person.class, 3);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));
        writer.write(new Person("c", 3));

        Path parquet = dir.resolve("batch_0001.parquet");
        assertThat(parquet).exists();
        assertThat(filesWithSuffix(dir, ".json")).isEmpty();
        assertThat(parquetRowCount(client, parquet)).isEqualTo(3);
        assertThat(writer.writtenItems()).isEqualTo(3);
    }

    @Test
    void flush_rollsUpPartialBatch(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, client, dir, Person.class, 10);

        writer.write(new Person("a", 1));
        writer.write(new Person("b", 2));

        // Below the batch size: nothing rolled up yet.
        assertThat(filesWithSuffix(dir, ".json")).hasSize(2);
        assertThat(filesWithSuffix(dir, ".parquet")).isEmpty();

        writer.flush();

        Path parquet = dir.resolve("batch_0001.parquet");
        assertThat(parquet).exists();
        assertThat(filesWithSuffix(dir, ".json")).isEmpty();
        assertThat(parquetRowCount(client, parquet)).isEqualTo(2);
    }

    @Test
    void multipleBatches_areNumbered_andFlushHandlesRemainder(@TempDir Path dir) throws IOException {
        JdbcClient client = duckDbClient();
        JsonItemWriter<Person> writer = new JsonItemWriter<>(objectMapper, client, dir, Person.class, 2);

        for (int i = 0; i < 5; i++) {
            writer.write(new Person("p" + i, i));
        }

        // Two full batches (4 items) rolled up; 1 item still buffered as JSON.
        assertThat(filesWithSuffix(dir, ".parquet")).hasSize(2);
        assertThat(filesWithSuffix(dir, ".json")).hasSize(1);

        writer.flush();

        assertThat(filesWithSuffix(dir, ".parquet")).hasSize(3);
        assertThat(filesWithSuffix(dir, ".json")).isEmpty();
        assertThat(writer.writtenItems()).isEqualTo(5);

        long totalRows = parquetRowCount(client, dir.resolve("batch_0001.parquet"))
                + parquetRowCount(client, dir.resolve("batch_0002.parquet"))
                + parquetRowCount(client, dir.resolve("batch_0003.parquet"));
        assertThat(totalRows).isEqualTo(5);
    }
}

