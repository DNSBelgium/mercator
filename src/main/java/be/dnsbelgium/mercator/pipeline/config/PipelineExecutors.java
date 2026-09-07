package be.dnsbelgium.mercator.pipeline.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;

/**
 * Immutable holder that bundles the module-agnostic concurrency machinery shared by
 * every pipeline instance:
 * <ul>
 *   <li>{@link #ioExecutor()} — virtual-thread-per-task executor for I/O-bound stages
 *       (producer, processors, writer);</li>
 *   <li>{@link #cpuPool()} — bounded platform-thread pool for CPU-bound work;</li>
 *   <li>{@link #watchdog()} — shared scheduler used to arm per-task timeout alarms;</li>
 *   <li>{@link #newSemaphore(int)} — factory for a per-pipeline backpressure semaphore
 *       so each module can bound its own outbound concurrency independently.</li>
 * </ul>
 *
 * <p>Passing this single object to a {@code PipelineService} keeps its constructor small
 * and makes it explicit that these resources are shared, not per-module.
 */
public record PipelineExecutors(
        Executor ioExecutor, ExecutorService cpuPool, ScheduledExecutorService watchdog) {

    /**
     * Creates a fresh backpressure semaphore. Each pipeline should own its own so
     * modules running concurrently do not contend for a single permit pool.
     *
     * @param permits maximum number of concurrent outbound requests
     */
    public Semaphore newSemaphore(int permits) {
        return new Semaphore(permits);
    }
}

