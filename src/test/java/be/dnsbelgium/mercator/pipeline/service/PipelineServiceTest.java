package be.dnsbelgium.mercator.pipeline.service;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.simulation.GeneratingItemSource;
import be.dnsbelgium.mercator.pipeline.simulation.SimulatedProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineServiceTest {

    /** A minimal in-memory writer; the pipeline drives a single writer thread, so no locking is needed. */
    private static final class CollectingWriter implements ItemWriter<String> {
        private final List<String> written = new CopyOnWriteArrayList<>();

        @Override
        public void write(String item) {
            written.add(item);
        }

        @Override
        public void flush() {
            // nothing to flush
        }

        @Override
        public int writtenItems() {
            return written.size();
        }

        List<String> items() {
            return written;
        }
    }

    @Test
    void processesEveryItemExactlyOnce_andShutsDownGracefully() {
        int itemCount = 200;

        CollectingWriter writer = new CollectingWriter();
        ItemSource<String> source = new GeneratingItemSource(0, itemCount);
        ItemProcessor<String, String> processor = new SimulatedProcessor();

        PipelineProperties properties = new PipelineProperties();
        properties.setNumConsumers(8);
        properties.setMaxConcurrentRequests(16);
        properties.setInputQueueCapacity(64);
        properties.setResultQueueCapacity(64);

        ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService cpuPool = Executors.newFixedThreadPool(2);
        ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1);
        PipelineExecutors executors = new PipelineExecutors(ioExecutor, cpuPool, watchdog);

        PipelineService<String, String> pipeline = new PipelineService<>(
                "test", source, processor, writer, executors, properties, new SimpleMeterRegistry());

        try {
            pipeline.runPipeline(properties.getNumConsumers());
        } finally {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }

        assertThat(writer.writtenItems()).isEqualTo(itemCount);

        // SimulatedProcessor returns "<thread>-processed-<original item>"; verify the set of
        // original items is covered exactly once (no loss, no duplication).
        List<String> originals = writer.items().stream()
                .map(result -> result.substring(result.indexOf("-processed-") + "-processed-".length()))
                .collect(Collectors.toList());
        List<String> expected = IntStream.range(0, itemCount).mapToObj(j -> "P0-Item-" + j).toList();

        assertThat(originals).containsExactlyInAnyOrderElementsOf(expected);
    }
}

