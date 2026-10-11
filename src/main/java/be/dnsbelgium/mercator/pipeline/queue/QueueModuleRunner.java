package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.PipelineModule;
import be.dnsbelgium.mercator.pipeline.service.StopSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sequential, round-robin runner for the stateful ({@code postgres-queue}) modules.
 * Instead of running the long-running DB-backed modules concurrently, it keeps
 * them <b>sequential</b> and relies on each module's {@link DatabaseItemSource}
 * being a <b>bounded pass</b>: one {@link PipelineModule#run(StopSignal)} leases at most
 * {@code max-items-per-pass} items (or stops early when its queue is empty) and returns.
 *
 * <p>The runner loops over the modules on a dedicated thread, running one pass each, and
 * only sleeps ({@code pipeline.queue.poll-interval}) when a <b>whole cycle produced no
 * work</b> — i.e. every module's queue is really empty — so it neither busy-loops when idle
 * nor delays a module that has work.
 *
 * <p><b>Graceful stop.</b> {@link #stop()} (triggered by the {@link ContextClosedEvent}, i.e.
 * <em>before</em> Spring destroys any bean the pass still needs: the datasources, DuckDB, the
 * HTTP client, …) raises the {@link StopSignal} and waits, at most
 * {@code pipeline.queue.shutdown-timeout}, for the running pass. The pass stops leasing, finishes
 * the items already in flight (writer flush → Parquet roll-up → ack) and <em>skips</em> the
 * leased-but-not-started items; those stay {@code RESERVED} and the {@link LeaseReaper} recycles
 * them once their lease expired. A pass is never interrupted. The idle sleep polls the stop flag
 * in small steps so shutdown stays responsive while the runner is idle.
 */
@Slf4j
@Component
@Profile("postgres-queue")
public class QueueModuleRunner {

    /** Granularity at which the idle sleep re-checks the stop flag, for responsive shutdown. */
    private static final Duration IDLE_SLEEP_STEP = Duration.ofMillis(200);

    private final Duration pollInterval;

    /** Bounded wait for the loop thread to finish its current pass on shutdown. */
    private final Duration shutdownTimeout;

    private final StopSignal stopSignal = new StopSignal();

    /** Makes {@link #stop()} run its body only once (a second call must not wait again). */
    private final AtomicBoolean stopCalled = new AtomicBoolean(false);

    /** Guarded by {@code this}: written by {@link #start}, read by {@link #stop()}. */
    private ExecutorService executor;

    public QueueModuleRunner(PipelineProperties properties) {
        this.pollInterval = properties.getQueue().getPollInterval();
        this.shutdownTimeout = properties.getQueue().getShutdownTimeout();
    }

    /**
     * Starts the round-robin loop on a dedicated (non-daemon) thread and returns immediately.
     * Called once by {@code PipelineApplication} after the context is ready. Does nothing when
     * the runner was already stopped.
     */
    public void start(List<PipelineModule> modules) {
        if (modules.isEmpty()) {
            log.warn("QueueModuleRunner started with no modules; nothing to do");
            return;
        }
        synchronized (this) {
            if (stopCalled.get()) {
                log.warn("QueueModuleRunner was already stopped; not starting");
                return;
            }
            this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "queue-module-runner"));
            this.executor.submit(() -> loop(modules));
        }
    }

    private void loop(List<PipelineModule> modules) {
        log.info("Continuous queue runner started for modules {} (pollInterval={})",
                modules.stream().map(PipelineModule::name).toList(), pollInterval);
        while (!stopSignal.isStopRequested()) {
            long producedThisCycle = 0;
            for (PipelineModule module : modules) {
                if (stopSignal.isStopRequested()) {
                    log.info("We were asked to stop");
                    break;
                }
                log.info("starting a pass of module '{}'", module.name());
                long start = System.currentTimeMillis();
                try {
                    long produced = module.run(stopSignal);
                    producedThisCycle += produced;
                    log.info("Module '{}' pass produced {} item(s) in {} ms",
                            module.name(), produced, System.currentTimeMillis() - start);
                } catch (RuntimeException e) {
                    log.error("Module '{}' pass failed", module.name(), e);
                }
            }
            if (!stopSignal.isStopRequested() && producedThisCycle == 0) {
                log.info("All module queues empty; sleeping {} before next cycle", pollInterval);
                idleSleep();
            }
        }
        log.info("Continuous queue runner stopped");
    }

    /** Sleeps up to {@link #pollInterval}, waking early (within {@link #IDLE_SLEEP_STEP}) if stopped. */
    private void idleSleep() {
        long remaining = pollInterval.toMillis();
        long step = IDLE_SLEEP_STEP.toMillis();
        while (!stopSignal.isStopRequested() && remaining > 0) {
            try {
                //noinspection BusyWait
                Thread.sleep(Math.min(step, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopSignal.requestStop();
                return;
            }
            remaining -= step;
        }
    }

    /**
     * Called when the application context starts closing. {@code ContextClosedEvent} is published
     * before lifecycle beans are stopped and before any singleton is destroyed, so everything the
     * in-flight pass needs (datasources, DuckDB, HTTP client, executors) is still alive while
     * {@link #stop()} waits for it.
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        stop();
    }

    /**
     * Stops the runner gracefully: no new passes, the running pass stops leasing, finishes its
     * in-flight items (flush, roll-up, ack) and skips the rest. Waits at most
     * {@code pipeline.queue.shutdown-timeout}.
     *
     * <p>Runs its body only once: a second call (or a call before {@link #start}) returns
     * immediately and never waits again.
     */
    public void stop() {
        if (!stopCalled.compareAndSet(false, true)) {
            log.debug("stop() was already called; ignoring");
            return;
        }
        long started = System.nanoTime();
        log.info("Stopping continuous queue runner: no new items are leased, in-flight items are finished, "
                + "flushed and acked (waiting at most {})", shutdownTimeout);
        ExecutorService runnerExecutor;
        synchronized (this) {
            stopSignal.requestStop();
            runnerExecutor = this.executor;
        }
        if (runnerExecutor == null) {
            return;
        }
        runnerExecutor.shutdown();
        try {
            if (runnerExecutor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.info("Queue runner stopped cleanly after {} ms",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            } else {
                log.warn("Queue runner did not stop within {}; proceeding with shutdown. The in-flight pass may "
                        + "still be running; its un-acked rows stay RESERVED and are recycled by the lease reaper",
                        shutdownTimeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for the queue runner to stop");
        }
    }
}

