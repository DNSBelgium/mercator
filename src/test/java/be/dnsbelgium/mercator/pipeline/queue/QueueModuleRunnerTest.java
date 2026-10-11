package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.PipelineModule;
import be.dnsbelgium.mercator.pipeline.service.StopSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/** Plain unit tests (no Spring, except the ordering test at the bottom) with fake modules. */
class QueueModuleRunnerTest {

    /** Released in {@link #tearDown()} so a module that ignores the signal never leaks a thread. */
    private final CountDownLatch releaseStubbornModules = new CountDownLatch(1);
    private QueueModuleRunner runner;

    @AfterEach
    void tearDown() {
        releaseStubbornModules.countDown();
        if (runner != null) {
            runner.stop();
        }
    }

    private QueueModuleRunner runner(Duration pollInterval, Duration shutdownTimeout) {
        PipelineProperties properties = new PipelineProperties();
        properties.getQueue().setPollInterval(pollInterval);
        properties.getQueue().setShutdownTimeout(shutdownTimeout);
        runner = new QueueModuleRunner(properties);
        return runner;
    }

    /** A module whose pass runs until the stop signal is raised, then takes {@code finishDelay} to finish. */
    private static final class CooperativeModule implements PipelineModule {
        final CountDownLatch entered = new CountDownLatch(1);
        final AtomicBoolean sawStop = new AtomicBoolean();
        final AtomicInteger passes = new AtomicInteger();
        private final Duration finishDelay;
        private final List<String> events;

        CooperativeModule(Duration finishDelay, List<String> events) {
            this.finishDelay = finishDelay;
            this.events = events;
        }

        @Override
        public String name() {
            return "cooperative";
        }

        @Override
        public long run() {
            throw new AssertionError("the runner must call run(StopSignal)");
        }

        @Override
        public long run(StopSignal stop) {
            passes.incrementAndGet();
            entered.countDown();
            while (!stop.isStopRequested()) {
                sleep(5);
            }
            sawStop.set(true);
            sleep(finishDelay.toMillis()); // "flush + roll-up + ack" of the in-flight items
            events.add("pass finished");
            return 1;
        }
    }

    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    void stop_propagatesStopSignalToRunningModule_andWaitsForThePassToFinish() throws Exception {
        CooperativeModule module = new CooperativeModule(Duration.ofMillis(200), new CopyOnWriteArrayList<>());
        QueueModuleRunner runner = runner(Duration.ofSeconds(30), Duration.ofSeconds(10));
        runner.start(List.of(module));
        assertThat(module.entered.await(5, TimeUnit.SECONDS)).isTrue();

        long start = System.nanoTime();
        assertTimeoutPreemptively(Duration.ofSeconds(5), runner::stop);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(module.sawStop).isTrue();
        assertThat(module.passes.get()).as("no second pass was started").isEqualTo(1);
        assertThat(elapsedMillis).as("stop() waited for the pass to finish").isGreaterThanOrEqualTo(150);
    }

    @Test
    void noNewPassIsStartedAfterStop() throws Exception {
        AtomicInteger passes = new AtomicInteger();
        CountDownLatch firstPass = new CountDownLatch(1);
        PipelineModule busy = new PipelineModule() {
            @Override
            public String name() {
                return "busy";
            }

            @Override
            public long run() {
                passes.incrementAndGet();
                firstPass.countDown();
                sleep(5);
                return 1; // always "had work": the loop never idles
            }
        };
        QueueModuleRunner runner = runner(Duration.ofSeconds(30), Duration.ofSeconds(10));
        runner.start(List.of(busy));
        assertThat(firstPass.await(5, TimeUnit.SECONDS)).isTrue();

        runner.stop();
        int passesAtStop = passes.get();
        sleep(200);

        assertThat(passes.get()).as("no pass after stop() returned").isEqualTo(passesAtStop);
    }

    @Test
    void stopWhileIdleSleeping_returnsQuickly() throws Exception {
        CountDownLatch firstPass = new CountDownLatch(1);
        PipelineModule empty = new PipelineModule() {
            @Override
            public String name() {
                return "empty";
            }

            @Override
            public long run() {
                firstPass.countDown();
                return 0; // nothing to do → the runner goes to its idle sleep
            }
        };
        QueueModuleRunner runner = runner(Duration.ofSeconds(60), Duration.ofSeconds(10));
        runner.start(List.of(empty));
        assertThat(firstPass.await(5, TimeUnit.SECONDS)).isTrue();
        sleep(100); // let it enter the 60 s idle sleep

        assertTimeoutPreemptively(Duration.ofSeconds(3), runner::stop);
    }

    @Test
    void stopIsIdempotent_andSafeBeforeStart_andPreventsALaterStart() {
        CooperativeModule module = new CooperativeModule(Duration.ZERO, new CopyOnWriteArrayList<>());
        QueueModuleRunner runner = runner(Duration.ofSeconds(30), Duration.ofSeconds(10));

        runner.stop();
        runner.stop();
        runner.start(List.of(module));

        sleep(100);
        assertThat(module.passes.get()).as("a stopped runner does not start").isZero();
    }

    @Test
    void stopTimeout_whenModuleIgnoresSignal_returnsAfterShutdownTimeout() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        QueueModuleRunner runner = runner(Duration.ofSeconds(30), Duration.ofMillis(300));
        runner.start(List.of(stubbornModule(entered)));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        long start = System.nanoTime();
        assertTimeoutPreemptively(Duration.ofSeconds(5), runner::stop);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsedMillis).as("waited for the shutdown timeout, then gave up").isBetween(250L, 3000L);
    }

    @Test
    void secondStopCall_doesNotWaitAgain() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        QueueModuleRunner runner = runner(Duration.ofSeconds(30), Duration.ofMillis(500));
        runner.start(List.of(stubbornModule(entered)));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        runner.stop(); // times out after 500 ms
        long start = System.nanoTime();
        runner.stop();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsedMillis).as("the once-only guard: no second wait").isLessThan(200);
    }

    @Test
    void onContextClosed_stopsTheRunner() throws Exception {
        CooperativeModule module = new CooperativeModule(Duration.ZERO, new CopyOnWriteArrayList<>());
        QueueModuleRunner runner = runner(Duration.ofSeconds(30), Duration.ofSeconds(10));
        runner.start(List.of(module));
        assertThat(module.entered.await(5, TimeUnit.SECONDS)).isTrue();

        runner.onContextClosed();

        assertThat(module.sawStop).isTrue();
    }

    @Test
    void anExceptionInAPass_doesNotPreventStopping() throws Exception {
        CountDownLatch failedOnce = new CountDownLatch(1);
        PipelineModule failing = new PipelineModule() {
            @Override
            public String name() {
                return "failing";
            }

            @Override
            public long run() {
                failedOnce.countDown();
                throw new IllegalStateException("simulated pass failure");
            }
        };
        QueueModuleRunner runner = runner(Duration.ofSeconds(60), Duration.ofSeconds(10));
        runner.start(List.of(failing));
        assertThat(failedOnce.await(5, TimeUnit.SECONDS)).isTrue();

        assertTimeoutPreemptively(Duration.ofSeconds(3), runner::stop);
    }

    /** A pass that never looks at the stop signal and only ends when the test is torn down. */
    private PipelineModule stubbornModule(CountDownLatch entered) {
        return new PipelineModule() {
            @Override
            public String name() {
                return "stubborn";
            }

            @Override
            public long run() {
                entered.countDown();
                try {
                    releaseStubbornModules.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 1;
            }
        };
    }

    // ---------------------------------------------------------------------------------------
    // Ordering: closing the context must wait for the pass BEFORE any bean it needs is destroyed.
    // ---------------------------------------------------------------------------------------

    /** Stands in for the datasource / HTTP client the in-flight pass still needs. */
    private record NeededByThePass(List<String> events) implements DisposableBean {
        @Override
        public void destroy() {
            events.add("dependency destroyed");
        }
    }

    @Test
    void closingTheContext_waitsForTheRunningPass_beforeDestroyingBeans() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        CooperativeModule module = new CooperativeModule(Duration.ofMillis(300), events);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("postgres-queue");
            context.registerBean(PipelineProperties.class, PipelineProperties::new);
            context.registerBean(QueueModuleRunner.class);
            context.registerBean(NeededByThePass.class, () -> new NeededByThePass(events));
            context.refresh();

            context.getBean(QueueModuleRunner.class).start(List.of(module));
            assertThat(module.entered.await(5, TimeUnit.SECONDS)).isTrue();

          //noinspection RedundantExplicitClose
          context.close(); // → ContextClosedEvent → runner.stop() (waits ≥ 300 ms) → destroy singletons
        }

        assertThat(events).containsExactly("pass finished", "dependency destroyed");
    }
}

