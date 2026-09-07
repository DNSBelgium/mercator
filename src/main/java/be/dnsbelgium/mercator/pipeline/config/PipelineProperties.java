package be.dnsbelgium.mercator.pipeline.config;

import be.dnsbelgium.mercator.pipeline.queue.DatabaseItemSource;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Tunable pipeline settings, bound from the {@code pipeline.*} namespace in
 * {@code application.properties}. All values have defaults that reproduce the
 * pipeline's previous hard-coded behavior, so the application runs identically
 * when nothing is configured.
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "pipeline")
public class PipelineProperties {

    /** Capacity of the bounded input queue between producer(s) and processors. */
    private int inputQueueCapacity = 2000;

    /** Capacity of the bounded result queue between processors and the writer. */
    private int resultQueueCapacity = 50;

    /** Number of concurrent processor (consumer) tasks. */
    private int numConsumers = 200;

    /** Upper bound on concurrent outbound requests (crawler politeness + resource cap). */
    private int maxConcurrentRequests = 200;

    /** Number of items per output batch (e.g. JSON files rolled up into one Parquet file). */
    private int batchSize = 500;

    /**
     * Bound on concurrent CPU-bound parsing tasks. Defaults to the number of
     * available processors so heavy work saturates the cores without oversubscribing.
     */
    private int cpuPoolSize = Runtime.getRuntime().availableProcessors();

    /** Wall-clock deadline for a single CPU-bound processing task. */
    private Duration taskTimeout = Duration.ofSeconds(30);

    /**
     * Number of synthetic items produced by the default simulated run (see
     * {@code PipelineApplication}). Ignored once real modules drive the pipeline.
     */
    private int simulatedItemCount = 258_000;

    /** Names of the modules to run on startup (e.g. {@code simulated}, {@code web}, {@code dns}). */
    private List<String> modules = List.of("simulated");

    /** CSV file (domain_name, visit_id) read by the CSV-backed modules (web/dns/smtp). */
    private String inputCsv = "input.csv";

    /** Base output directory; each module writes to a {@code <outputDirectory>/<module>} subdir. */
    private String outputDirectory = "output";

    /** Stateful Postgres work-queue settings (only used under the {@code postgres-queue} profile). */
    private final Queue queue = new Queue();

    /**
     * Settings for the stateful Postgres-backed work queue: fan-out ({@code dispatch()}),
     * leasing ({@code getItems()}), and the lease reaper. Bound from {@code pipeline.queue.*}.
     */
    @Getter
    @Setter
    @ToString
    public static class Queue {

        /** Master switch for the stateful queue (dispatcher + reaper scheduling). */
        private boolean enabled = true;

        /** Lease duration; a {@code RESERVED} row whose {@code reserved_timestamp} is older is reclaimable. */
        private Duration leaseDuration = Duration.ofMinutes(10);

        /** Rows leased per {@code getItems()} poll (bounded fetch = backpressure). */
        private int fetchSize = 500;

        /**
         * Per-module pass budget. A {@link DatabaseItemSource}
         * pass ends once it has produced this many items <em>or</em> its queue is empty, whichever
         * comes first, so the sequential runner can advance to the next module. The last claim's
         * {@code LIMIT} is clamped so a pass never leases more than this.
         */
        private int maxItemsPerPass = 2000;

        /**
         * How long the sequential {@code QueueModuleRunner} sleeps after a full cycle over all
         * modules produced no work (i.e. every queue is really empty), avoiding a busy loop.
         */
        private Duration pollInterval = Duration.ofSeconds(10);

        /** Attempts before an expired lease is dead-lettered to {@code FAILED} instead of recycled. */
        private int maxAttempts = 5;

        /** Crawler modules that {@code dispatch()} fans each visit out to. */
        private List<String> crawlerModules = List.of("web", "dns", "smtp");

        /** Cadence of the {@code @Scheduled dispatch()} fan-out. */
        private Duration dispatchInterval = Duration.ofSeconds(30);

        /** Cadence of the {@code @Scheduled} lease reaper (recycle expired leases / dead-letter). */
        private Duration reaperInterval = Duration.ofMinutes(1);

        /** Stable per-worker id written to {@code reserved_by} for diagnostics. */
        private String instanceId = defaultInstanceId();

        private static String defaultInstanceId() {
            String host = System.getenv("HOSTNAME");
            if (host != null && !host.isBlank()) {
                return host;
            }
            try {
                return java.net.InetAddress.getLocalHost().getHostName();
            } catch (java.net.UnknownHostException e) {
                return "unknown-" + java.util.UUID.randomUUID();
            }
        }
    }
}

