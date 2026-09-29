package be.dnsbelgium.mercator.pipeline.simulation;

import be.dnsbelgium.mercator.pipeline.service.ItemWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy text {@link ItemWriter} that preserves the original behavior: it buffers items
 * and flushes a numbered batch file ({@code output_batch_%04d.txt}) every {@code batchSize}
 * items, one item per line.
 *
 * <p>Not thread-safe by design: the pipeline drives a single writer thread, so the buffer
 * needs no synchronization.
 */
@Slf4j
public class FileWriterService implements ItemWriter<String> {

    private final Path outputDirectory;
    private final int batchSize;
    private final List<String> buffer;

    private int fileCounter = 1;
    private int written = 0;

    public FileWriterService(Path outputDirectory, int batchSize) {
        this.outputDirectory = outputDirectory;
        this.batchSize = batchSize;
        this.buffer = new ArrayList<>(batchSize);
        boolean ignored = outputDirectory.toFile().mkdirs();
    }

    @Override
    public void write(String item) {
        buffer.add(item);
        written++;
        if (buffer.size() >= batchSize) {
            writeBatch();
        }
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            writeBatch();
        }
    }

    @Override
    public int writtenItems() {
        return written;
    }

    private void writeBatch() {
        Path file = outputDirectory.resolve(String.format("output_batch_%04d.txt", fileCounter++));
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (String item : buffer) {
                writer.write(item);
                writer.newLine();
            }
            log.info("Created batch file: {} containing {} items.", file, buffer.size());
        } catch (IOException e) {
            log.error("Failed to write to file {}: {}", file, e.getMessage());
        } finally {
            buffer.clear();
        }
    }
}
