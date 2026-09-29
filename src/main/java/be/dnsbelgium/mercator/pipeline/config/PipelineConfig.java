package be.dnsbelgium.mercator.pipeline.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(PipelineProperties.class)
@Slf4j
public class PipelineConfig {

    /**
     * Virtual-thread-per-task executor. Ideal for I/O-bound pipeline stages
     * (web crawling with OkHttp, file writing): every blocking call parks the
     * virtual thread and frees its carrier OS thread, so thousands of tasks can be
     * in flight without exhausting a fixed platform-thread pool.
     * <p>
     * Rules: never pool virtual threads and never set core/max sizes. On Java 24+
     * (JEP 491) synchronized no longer pins the carrier during I/O, so OkHttp is safe.
     */
    @Bean(name = "ioExecutor")
    public ExecutorService ioExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Bounded platform-thread pool for CPU-bound parsing (heavy regex over large/deep
     * DOMs). Runs the CPU work on dedicated OS threads so it never starves the
     * virtual-thread carriers that serve blocking I/O, and caps CPU concurrency at
     * {@link PipelineProperties#getCpuPoolSize()}. Threads are named for easy diagnostics.
     */
    @Bean(name = "cpuPool", destroyMethod = "shutdown")
    public ExecutorService cpuPool(PipelineProperties properties) {
        AtomicInteger seq = new AtomicInteger();
        return Executors.newFixedThreadPool(properties.getCpuPoolSize(), r -> {
            Thread t = new Thread(r, "cpu-pool-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Tiny shared scheduler used only to arm per-task watchdogs that interrupt a
     * parsing thread if it overruns its deadline. The alarm is scheduled from inside
     * the task, so the timeout starts when the worker actually begins — not at submit
     * time. Daemon threads so it never blocks JVM shutdown.
     */
    @Bean(name = "watchdog", destroyMethod = "shutdown")
    public ScheduledExecutorService watchdog() {
        return Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "cpu-pool-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Bundles the shared, module-agnostic concurrency machinery into a single object so
     * a (generic) pipeline can be constructed with one dependency instead of four. Each
     * pipeline creates its own backpressure semaphore via
     * {@link PipelineExecutors#newSemaphore(int)}; the executors here are shared.
     */
    @Bean
    public PipelineExecutors pipelineExecutors(@Qualifier("ioExecutor") Executor ioExecutor,
                                               @Qualifier("cpuPool") ExecutorService cpuPool,
                                               @Qualifier("watchdog") ScheduledExecutorService watchdog) {
        return new PipelineExecutors(ioExecutor, cpuPool, watchdog);
    }
}
