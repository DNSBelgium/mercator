package be.dnsbelgium.mercator.pipeline.service;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.simulation.GeneratingItemSource;
import be.dnsbelgium.mercator.pipeline.simulation.SimulatedProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.awaitUninterruptibly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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

    /** A source that fails on the first poll, e.g. because its input file (input.csv) is missing. */
    private static final class FailingItemSource implements ItemSource<String> {
        @Override
        public List<String> getItems() {
            throw new IllegalStateException("simulated failure: input.csv not found");
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean sleepBetweenPolls() {
            return false;
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
                "test", source, processor, writer, executors, properties, new SimpleMeterRegistry(),
                properties.maxConcurrentRequests("test"));

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

    @Test
    void failsPromptlyWhenProducerSourceFails_insteadOfHangingForever() {
        CollectingWriter writer = new CollectingWriter();
        ItemSource<String> source = new FailingItemSource();
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
                "failing", source, processor, writer, executors, properties, new SimpleMeterRegistry(),
                properties.maxConcurrentRequests("failing"));

        try {
            // A failing source must stop the consumers/writer and propagate the failure so a
            // batch entrypoint cannot mark unread input as successfully processed.
            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThatThrownBy(() -> pipeline.runPipeline(properties.getNumConsumers()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Producer for pipeline 'failing' failed")
                            .hasRootCauseMessage("simulated failure: input.csv not found"));
            assertThat(writer.writtenItems()).isZero();
        } finally {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }
    }

    @Test
    void failsAfterShutdownWhenProcessorFails() {
        CollectingWriter writer = new CollectingWriter();
        ItemSource<String> source = new GeneratingItemSource(0, 3);
        ItemProcessor<String, String> processor = item -> {
            if (item.endsWith("1")) {
                throw new IllegalStateException("simulated processor failure");
            }
            return item;
        };

        PipelineProperties properties = new PipelineProperties();
        properties.setNumConsumers(2);
        properties.setMaxConcurrentRequests(2);
        properties.setInputQueueCapacity(4);
        properties.setResultQueueCapacity(4);

        ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService cpuPool = Executors.newFixedThreadPool(1);
        ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1);
        PipelineExecutors executors = new PipelineExecutors(ioExecutor, cpuPool, watchdog);
        PipelineService<String, String> pipeline = new PipelineService<>(
                "failing", source, processor, writer, executors, properties, new SimpleMeterRegistry(),
                properties.maxConcurrentRequests("failing"));

        try {
            assertThatThrownBy(() -> pipeline.runPipeline(properties.getNumConsumers()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Processor for pipeline 'failing' failed")
                    .hasRootCauseMessage("simulated processor failure");
            assertThat(writer.items()).containsExactlyInAnyOrder("P0-Item-0", "P0-Item-2");
        } finally {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Graceful stop: in-flight items finish (write + flush), everything not started is skipped.
    // ---------------------------------------------------------------------------------------

    @Test
    void stopMidPass_inFlightItemsFinish_queuedItemsAreSkipped() throws Exception {
        int consumers = 4;
        ListItemSource source = new ListItemSource(100, 10);
        RecordingWriter writer = new RecordingWriter();
        StopSignal stop = new StopSignal();

        CountDownLatch allConsumersBusy = new CountDownLatch(consumers);
        CountDownLatch release = new CountDownLatch(1);
        List<String> started = new CopyOnWriteArrayList<>();
        ItemProcessor<String, String> blocking = item -> {
            started.add(item);
            allConsumersBusy.countDown();
            awaitUninterruptibly(release);
            return item;
        };

        try (Harness harness = new Harness()) {
            PipelineProperties properties = properties(consumers, 200);
            PipelineService<String, String> pipeline =
                    harness.pipeline("stop-mid-pass", source, blocking, writer, properties, stop);
            CompletableFuture<Long> run = CompletableFuture.supplyAsync(() -> pipeline.runPipeline(consumers));

            // Every consumer is inside the processor; the other items are queued or not even leased yet.
            assertThat(allConsumersBusy.await(10, TimeUnit.SECONDS)).isTrue();
            stop.requestStop();
            release.countDown();

            run.get(10, TimeUnit.SECONDS);
        }

        assertThat(started).as("only the items that were already in flight were started").hasSize(consumers);
        assertThat(writer.written).as("their results were written").containsExactlyInAnyOrderElementsOf(started);
        assertThat(writer.writtenAtFlush).as("flush happened after the last in-flight write").isEqualTo(consumers);
        assertThat(source.closed).isTrue();
    }

    @Test
    void stopBeforeStart_leasesNothing() {
        ListItemSource source = new ListItemSource(10, 5);
        RecordingWriter writer = new RecordingWriter();
        AtomicInteger processed = new AtomicInteger();
        StopSignal stop = new StopSignal();
        stop.requestStop();

        try (Harness harness = new Harness()) {
            PipelineService<String, String> pipeline = harness.pipeline("stop-before-start", source,
                    item -> {
                        processed.incrementAndGet();
                        return item;
                    }, writer, properties(2, 16), stop);

            long produced = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> pipeline.runPipeline(2));

            assertThat(produced).isZero();
        }
        assertThat(source.polls.get()).as("nothing was leased").isZero();
        assertThat(processed.get()).isZero();
        assertThat(writer.written).isEmpty();
        assertThat(writer.writtenAtFlush).as("the writer was still flushed").isZero();
        assertThat(source.closed).isTrue();
    }

    @Test
    void stopWhileProducerBlockedOnFullInputQueue_doesNotHang() throws Exception {
        // 1 consumer stuck in the processor, input queue of 2: the producer blocks on the 4th item.
        ListItemSource source = new ListItemSource(50, 50);
        RecordingWriter writer = new RecordingWriter();
        StopSignal stop = new StopSignal();

        CountDownLatch processorEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> started = new CopyOnWriteArrayList<>();
        ItemProcessor<String, String> blocking = item -> {
            started.add(item);
            processorEntered.countDown();
            awaitUninterruptibly(release);
            return item;
        };

        try (Harness harness = new Harness()) {
            PipelineProperties properties = properties(1, 2);
            PipelineService<String, String> pipeline =
                    harness.pipeline("stop-blocked-producer", source, blocking, writer, properties, stop);
            CompletableFuture<Long> run = CompletableFuture.supplyAsync(() -> pipeline.runPipeline(1));

            assertThat(processorEntered.await(10, TimeUnit.SECONDS)).isTrue();
            TimeUnit.MILLISECONDS.sleep(300); // let the producer fill the queue and block
            stop.requestStop();
            release.countDown();

            run.get(10, TimeUnit.SECONDS);
        }

        assertThat(started).as("everything queued behind the in-flight item was skipped").hasSize(1);
        assertThat(writer.written).containsExactlyElementsOf(started);
    }

    @Test
    void stopDuringClaim_theJustLeasedBatchIsNotEnqueued() throws Exception {
        BlockingClaimSource source = new BlockingClaimSource();
        RecordingWriter writer = new RecordingWriter();
        AtomicInteger processed = new AtomicInteger();
        StopSignal stop = new StopSignal();

        try (Harness harness = new Harness()) {
            PipelineService<String, String> pipeline = harness.pipeline("stop-during-claim", source,
                    item -> {
                        processed.incrementAndGet();
                        return item;
                    }, writer, properties(2, 16), stop);
            CompletableFuture<Long> run = CompletableFuture.supplyAsync(() -> pipeline.runPipeline(2));

            assertThat(source.claimEntered.await(10, TimeUnit.SECONDS)).isTrue();
            stop.requestStop();
            source.claimMayReturn.countDown();

            assertThat(run.get(10, TimeUnit.SECONDS)).as("nothing was enqueued").isZero();
        }
        assertThat(processed.get()).isZero();
        assertThat(writer.written).isEmpty();
        assertThat(source.closed).isTrue();
    }

    @Test
    void stopWakesAProducerThatSleepsBetweenPolls() throws Exception {
        PollingSource source = new PollingSource();
        StopSignal stop = new StopSignal();

        try (Harness harness = new Harness()) {
            PipelineService<String, String> pipeline = harness.pipeline("stop-sleeping-producer", source,
                    item -> item, new RecordingWriter(), properties(2, 16), stop);
            CompletableFuture<Long> run = CompletableFuture.supplyAsync(() -> pipeline.runPipeline(2));

            assertThat(source.polled.await(10, TimeUnit.SECONDS)).isTrue();
            stop.requestStop();

            // Without the stop-aware sleep this would take the full 10 s sleep.
            run.get(3, TimeUnit.SECONDS);
        }
        assertThat(source.closed).isTrue();
    }

    // --- helpers for the stop tests ------------------------------------------------------

    private static PipelineProperties properties(int consumers, int inputQueueCapacity) {
        PipelineProperties properties = new PipelineProperties();
        properties.setNumConsumers(consumers);
        properties.setMaxConcurrentRequests(consumers);
        properties.setInputQueueCapacity(inputQueueCapacity);
        properties.setResultQueueCapacity(Math.max(16, inputQueueCapacity));
        return properties;
    }

    /** Owns the executors a pipeline needs and shuts them down when the test is done. */
    private static final class Harness implements AutoCloseable {
        private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
        private final ExecutorService cpuPool = Executors.newFixedThreadPool(1);
        private final ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1);
        private final PipelineExecutors executors = new PipelineExecutors(ioExecutor, cpuPool, watchdog);

        PipelineService<String, String> pipeline(String name,
                                                 ItemSource<String> source,
                                                 ItemProcessor<String, String> processor,
                                                 ItemWriter<String> writer,
                                                 PipelineProperties properties,
                                                 StopSignal stop) {
            return new PipelineService<>(name, source, processor, writer, executors, properties,
                    new SimpleMeterRegistry(), properties.maxConcurrentRequests(name), stop);
        }

        @Override
        public void close() {
            ioExecutor.shutdownNow();
            cpuPool.shutdownNow();
            watchdog.shutdownNow();
        }
    }

    /** Writer that remembers how many items had been written when {@code flush()} was called. */
    private static final class RecordingWriter implements ItemWriter<String> {
        private final List<String> written = new CopyOnWriteArrayList<>();
        private volatile int writtenAtFlush = -1;

        @Override
        public void write(String item) {
            written.add(item);
        }

        @Override
        public void flush() {
            writtenAtFlush = written.size();
        }

        @Override
        public int writtenItems() {
            return written.size();
        }
    }

    /** Source that hands out {@code count} items in batches of {@code batchSize}, like a bounded DB pass. */
    private static final class ListItemSource implements ItemSource<String> {
        private final List<String> items;
        private final int batchSize;
        private final AtomicInteger polls = new AtomicInteger();
        private int next = 0;
        private volatile boolean closed;

        ListItemSource(int count, int batchSize) {
            this.items = IntStream.range(0, count).mapToObj(i -> "item-" + i).toList();
            this.batchSize = batchSize;
        }

        @Override
        public List<String> getItems() {
            polls.incrementAndGet();
            int to = Math.min(next + batchSize, items.size());
            List<String> batch = List.copyOf(items.subList(next, to));
            next = to;
            return batch;
        }

        @Override
        public boolean isDone() {
            return next >= items.size();
        }

        @Override
        public boolean sleepBetweenPolls() {
            return false;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** Source whose single claim blocks until the test lets it return a batch. */
    private static final class BlockingClaimSource implements ItemSource<String> {
        private final CountDownLatch claimEntered = new CountDownLatch(1);
        private final CountDownLatch claimMayReturn = new CountDownLatch(1);
        private volatile boolean delivered;
        private volatile boolean closed;

        @Override
        public List<String> getItems() {
            claimEntered.countDown();
            awaitUninterruptibly(claimMayReturn);
            delivered = true;
            return List.of("a", "b", "c", "d", "e");
        }

        @Override
        public boolean isDone() {
            return delivered;
        }

        @Override
        public boolean sleepBetweenPolls() {
            return false;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** A dynamic source that never has work and wants the producer to sleep between polls. */
    private static final class PollingSource implements ItemSource<String> {
        private final CountDownLatch polled = new CountDownLatch(1);
        private volatile boolean closed;

        @Override
        public List<String> getItems() {
            polled.countDown();
            return List.of();
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
