package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.PipelineModule;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Sequential, round-robin runner for the stateful ({@code postgres-queue}) modules.
 * Instead of running the long-running DB-backed modules concurrently, it keeps
 * them <b>sequential</b> and relies on each module's {@link DatabaseItemSource}
 * being a <b>bounded pass</b>: one {@link PipelineModule#run()} leases at most
 * {@code max-items-per-pass} items (or stops early when its queue is empty) and returns.
 *
 * <p>The runner loops over the modules on a dedicated thread, running one pass each, and
 * only sleeps ({@code pipeline.queue.poll-interval}) when a <b>whole cycle produced no
 * work</b> — i.e. every module's queue is really empty — so it neither busy-loops when idle
 * nor delays a module that has work.
 *
 * <p>Shutdown ({@link #stop()}) is graceful: it stops the loop but never interrupts an
 * in-flight pass, so the current pass finishes cleanly (poison pill → writer flush → Parquet
 * roll-up → ack). The idle sleep polls {@code running} in small steps so shutdown stays
 * responsive even while the runner is idle.
 */
@Slf4j
@Component
@Profile("postgres-queue")
public class QueueModuleRunner {

    /** Granularity at which the idle sleep re-checks {@code running}, for responsive shutdown. */
    private static final Duration IDLE_SLEEP_STEP = Duration.ofMillis(200);

    /** Bounded wait for the loop thread to finish its current pass on shutdown. */
    private static final Duration SHUTDOWN_JOIN_TIMEOUT = Duration.ofSeconds(60);

    private final Duration pollInterval;

    private volatile boolean running = true;
    private ExecutorService executor;

    public QueueModuleRunner(PipelineProperties properties) {
        this.pollInterval = properties.getQueue().getPollInterval();
    }

    /**
     * Starts the round-robin loop on a dedicated (non-daemon) thread and returns immediately.
     * Called once by {@code PipelineApplication} after the context is ready.
     */
    public void start(List<PipelineModule> modules) {
        if (modules.isEmpty()) {
            log.warn("QueueModuleRunner started with no modules; nothing to do");
            return;
        }
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "queue-module-runner"));
        this.executor.submit(() -> loop(modules));
    }

    private void loop(List<PipelineModule> modules) {
        log.info("Continuous queue runner started for modules {} (pollInterval={})",
                modules.stream().map(PipelineModule::name).toList(), pollInterval);
        while (running) {
            long producedThisCycle = 0;
            for (PipelineModule module : modules) {
                if (!running) {
                    break;
                }
                long start = System.currentTimeMillis();
                try {
                    long produced = module.run();
                    producedThisCycle += produced;
                    log.info("Module '{}' pass produced {} item(s) in {} ms",
                            module.name(), produced, System.currentTimeMillis() - start);
                } catch (RuntimeException e) {
                    log.error("Module '{}' pass failed", module.name(), e);
                }
            }
            if (running && producedThisCycle == 0) {
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
        while (running && remaining > 0) {
            try {
                //noinspection BusyWait
                Thread.sleep(Math.min(step, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                return;
            }
            remaining -= step;
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping continuous queue runner (letting the current pass finish)");
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_JOIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    log.warn("Queue runner did not stop within {}; proceeding with shutdown", SHUTDOWN_JOIN_TIMEOUT);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

