package be.dnsbelgium.mercator.pipeline.service;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic, reusable producer → processor → writer pipeline.
 *
 * <p>An instance is constructed per module (web, DNS, SMTP, …) from three module-specific
 * seams and the shared concurrency machinery:
 * <ul>
 *   <li>{@link ItemSource}{@code <InputType>} — the producer: reads work items;</li>
 *   <li>{@link ItemProcessor}{@code <InputType, OutputType>} — does the actual work, run on many virtual
 *       threads;</li>
 *   <li>{@link ItemWriter}{@code <OutputType>} — the consumer: a single thread that persists results.</li>
 * </ul>
 *
 * <p>Two bounded queues decouple the stages, carrying a {@link Signal} envelope so a typed
 * poison pill can travel with the payloads:
 * <pre>
 *   ItemSource → [inputQueue] → ItemProcessor(s) → [resultQueue] → ItemWriter
 * </pre>
 *
 * @param <InputType> input item type read from the {@link ItemSource}
 * @param <OutputType> output item type produced by the {@link ItemProcessor} and persisted by the {@link ItemWriter}
 */
@Slf4j
public class PipelineService<InputType, OutputType> {

    private final String name;
    private final ItemSource<InputType> source;
    private final ItemProcessor<InputType, OutputType> processor;
    private final ItemWriter<OutputType> writer;

    private final BlockingQueue<Signal<InputType>> inputQueue;
    private final BlockingQueue<Signal<OutputType>> resultQueue;

    private final Executor ioExecutor;
    private final Semaphore crawlSemaphore;
    private final ExecutorService cpuPool;
    private final ScheduledExecutorService watchdog;

    /** Items the producer read from the source and enqueued during the current run. */
    private final AtomicLong producedCount = new AtomicLong();

    public PipelineService(String name,
                           ItemSource<InputType> source,
                           ItemProcessor<InputType, OutputType> processor,
                           ItemWriter<OutputType> writer,
                           PipelineExecutors executors,
                           PipelineProperties properties,
                           MeterRegistry meterRegistry) {
        this.name = name;
        this.source = source;
        this.processor = processor;
        this.writer = writer;
        this.ioExecutor = executors.ioExecutor();
        this.cpuPool = executors.cpuPool();
        this.watchdog = executors.watchdog();
        // Per-pipeline backpressure so modules running concurrently don't share permits.
        this.crawlSemaphore = executors.newSemaphore(properties.getMaxConcurrentRequests());
        this.inputQueue = new ArrayBlockingQueue<>(properties.getInputQueueCapacity());
        this.resultQueue = new ArrayBlockingQueue<>(properties.getResultQueueCapacity());

        // Per-module queue-depth gauges (e.g. pipeline.input.queue.size{module="web"}).
        Tags tags = Tags.of("module", name);
        meterRegistry.gauge("pipeline.input.queue.size", tags, inputQueue, BlockingQueue::size);
        meterRegistry.gauge("pipeline.result.queue.size", tags, resultQueue, BlockingQueue::size);
    }

    /**
     * Runs the pipeline to completion: starts the single writer, the single producer and
     * {@code numConsumers} processors, then blocks until every result has been written.
     *
     * <p>Shutdown is coordinated with poison pills:
     * <ol>
     *   <li>the producer appends one {@link Signal.Poison} to {@code inputQueue} once the
     *       source is drained; each processor re-inserts it for its siblings before exiting;</li>
     *   <li>when all processors have exited, one {@link Signal.Poison} is placed on
     *       {@code resultQueue} to stop the writer, which then {@link ItemWriter#flush() flushes}.</li>
     * </ol>
     *
     * @return the number of items the producer read from the source and enqueued this run
     *         (used by the sequential runner to detect an idle cycle)
     */
    public long runPipeline(int numConsumers) {
        CompletableFuture<Void> writerFuture = CompletableFuture.runAsync(this::writeResults, ioExecutor);

        CompletableFuture<Void> producerFuture = CompletableFuture.runAsync(this::produceData, ioExecutor)
            .exceptionally(ex -> {
                log.error("Producer failed.", ex);
                return null;
            });

        List<CompletableFuture<Void>> consumerFutures = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            consumerFutures.add(CompletableFuture.runAsync(this::processItems, ioExecutor));
        }

        CompletableFuture.allOf(toArray(consumerFutures))
            .thenRun(() -> {
                log.info("Consumers done. Sending result poison pill.");
                putUninterruptibly(resultQueue, new Signal.Poison<>());
            })
            .exceptionally(ex -> {
                log.error("Failed to signal end of result writing.", ex);
                return null;
            });

        // Waiting on the writer transitively waits for producer + consumers, since the
        // writer only stops after the result poison pill, which is only sent once every
        // consumer has drained the input (including the producer's poison pill).
        writerFuture.join();
        producerFuture.join();
        log.info("Pipeline '{}' finished. Wrote {} items.", name, writer.writtenItems());
        return producedCount.get();
    }

    /**
     * Producer stage. Drains the {@link ItemSource} and enqueues each item wrapped in a
     * {@link Signal.Payload}; appends a single {@link Signal.Poison} when the source is done.
     */
    private void produceData() {
        try {
            while (!source.isDone()) {
                List<InputType> items = source.getItems();
                for (InputType item : items) {
                    inputQueue.put(Signal.payload(item));
                    producedCount.incrementAndGet();
                }
                if (items.isEmpty() && source.sleepBetweenPolls()) {
                    log.info("Producer polled no items; sleeping for 10 seconds before next poll.");
                    Thread.sleep(Duration.ofSeconds(10));
                }
            }
            inputQueue.put(Signal.poison());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            source.close();
        }
    }

    /**
     * Processor stage. Runs on many virtual threads. Each takes items until it sees the
     * poison pill, which it re-inserts so sibling processors also stop.
     */
    private void processItems() {
        try {
            while (true) {
                Signal<InputType> signal = inputQueue.take();
                if (signal instanceof Signal.Payload<InputType>(InputType value)) {
                    OutputType result = processOne(value);
                    if (result != null) {
                        resultQueue.put(new Signal.Payload<>(result));
                    }
                } else {
                    // Poison: let the other processors see it, then stop.
                    inputQueue.put(signal);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes a single item on the current (virtual) thread, bounded by
     * {@link #crawlSemaphore} so outbound concurrency stays capped. Blocking InputType/OutputType inside a
     * processor parks the virtual thread and frees its carrier, so thousands can be in
     * flight. CPU-heavy sub-steps inside a processor can be offloaded via
     * {@link #runWithTimeout(Callable, Duration)}.
     *
     * <p>A failure is logged and the item is skipped (returns {@code null}) so one bad item
     * never takes down a processor thread.
     */
    private OutputType processOne(InputType item) throws InterruptedException {
        crawlSemaphore.acquire();
        try {
            return processor.processItem(item);
        } catch (RuntimeException e) {
            log.error("Processing failed for item {}", item, e);
            return null;
        } finally {
            crawlSemaphore.release();
        }
    }

    /**
     * Writer stage. A single thread drains {@code resultQueue}, writing each item, and
     * {@link ItemWriter#flush() flushes} once the poison pill arrives.
     */
    private void writeResults() {
        try {
            while (true) {
                Signal<OutputType> signal = resultQueue.take();
                if (signal instanceof Signal.Payload<OutputType>(OutputType value)) {
                    writer.write(value);
                } else {
                    log.info("Writer received shutdown signal.");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writer.flush();
        }
    }

    /**
     * Converts a list of futures to the array form required by
     * {@link CompletableFuture#allOf}. Uses an unbounded-wildcard element type so no
     * generic array is created and the assignment stays unchecked-warning free.
     */
    private static CompletableFuture<?>[] toArray(List<? extends CompletableFuture<?>> futures) {
        return futures.toArray(new CompletableFuture<?>[0]);
    }

    /**
     * Puts an element into a bounded blocking queue, blocking (via {@code put}) until
     * space is available instead of throwing when the queue is full (as {@code add} would).
     * Restores the interrupt flag if interrupted while waiting.
     */
    private static <T> void putUninterruptibly(BlockingQueue<T> queue, T element) {
        try {
            queue.put(element);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while enqueueing poison pill: {}", element, e);
            throw new IllegalStateException("Interrupted while enqueueing poison pill", e);
        }
    }

    /**
     * Runs a CPU-bound task on the bounded {@code cpuPool}, enforcing a wall-clock
     * {@code limit} that starts when a worker actually begins the task (not when it is
     * submitted). If the task overruns, the worker thread is interrupted.
     *
     * <p>The timer is armed from <em>inside</em> the submitted task via the shared
     * {@code watchdog}, so time spent queued waiting for a free CPU thread does not
     * count against the deadline. The outer {@link Future#get()} has no timeout — the
     * watchdog governs runtime.
     *
     * <p>Intended for CPU-heavy sub-steps of an {@link ItemProcessor} (e.g. regex/DOM
     * parsing) that should be capped and kept off the virtual-thread carriers. The engine
     * itself invokes {@link ItemProcessor#processItem(Object)} directly on the virtual
     * thread; a processor opts into this helper for its parsing section.
     *
     * <p><strong>Caveat:</strong> interruption is cooperative. The task must observe it
     * (blocking calls, or explicit {@code Thread.isInterrupted()} checks). Plain
     * {@code java.util.regex} matching does <em>not</em> poll the interrupt flag, so a
     * catastrophic-backtracking regex can still overrun the deadline until it either
     * finishes or reaches a point that checks interruption.
     *
     * @throws TimeoutException  if the task was interrupted after exceeding {@code limit}
     * @throws InterruptedException if the calling thread is interrupted while waiting
     * @throws Exception any exception thrown by the task itself
     */
    <T> T runWithTimeout(Callable<T> task, Duration limit) throws Exception {
        Future<T> future = cpuPool.submit(() -> {
            // Timer starts HERE — when the worker actually begins the task.
            final Thread worker = Thread.currentThread();
            ScheduledFuture<?> alarm = watchdog.schedule(
                    worker::interrupt, limit.toMillis(), TimeUnit.MILLISECONDS);
            try {
                return task.call();
            } finally {
                // Completed (or failed) in time → disarm the alarm. If it already
                // fired, clear the possibly-set interrupt flag so the pool thread is
                // clean for its next task.
                alarm.cancel(false);
                boolean ignored = Thread.interrupted();
            }
        });

        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // A deadline breach surfaces as the task being interrupted.
            if (cause instanceof InterruptedException) {
                future.cancel(true);
                throw new TimeoutException("Task exceeded time limit of " + limit);
            }
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }
}
