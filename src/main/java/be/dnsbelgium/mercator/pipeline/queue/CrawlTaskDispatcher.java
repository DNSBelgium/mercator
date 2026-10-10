package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fans each undispatched {@code visit_requests} row out into one {@code crawl_tasks} row
 * per enabled crawler module (the {@code pipeline.queue.crawler-modules} list).
 *
 * <p>Idempotent, non-destructive fan-out via a per-run UUID <b>claim token</b> (§3 of the
 * plan) — no {@code ON CONFLICT}, no destructive replace, portable across Postgres / DuckDB
 * / GizmoSQL:
 * <ol>
 *   <li>stamp every {@code dispatched_id is null} visit with a fresh {@code dispatch_id}
 *       (the token) and {@code dispatched_at = now()};</li>
 *   <li>copy exactly the rows carrying that token into {@code crawl_tasks}, cross-joined
 *       with the enabled modules.</li>
 * </ol>
 * Both statements run in one transaction ({@code queueTransactionManager}) so a crash rolls
 * back cleanly; already-dispatched visits are skipped, so re-runs create no duplicates.
 */
@SuppressWarnings("SqlResolve")
@Slf4j
@Component
@Profile("postgres-queue")
public class CrawlTaskDispatcher {

    /** Module names must be simple identifiers — they are inlined into the VALUES clause. */
    private static final Pattern MODULE_NAME = Pattern.compile("[a-z0-9_]+");

    private static final String STAMP_SQL = """
            update visit_requests
            set    dispatch_id = :token, dispatched_at = now()
            where  dispatched_at is null
            """;

    private final JdbcClient jdbcClient;
    private final List<String> modules;
    private final String fanOutSql;

    public CrawlTaskDispatcher(@Qualifier("queueJdbcClient") JdbcClient queueJdbcClient,
                               PipelineProperties properties) {
        this.jdbcClient = queueJdbcClient;
        this.modules = List.copyOf(properties.getQueue().getCrawlerModules());
        this.fanOutSql = buildFanOutSql(this.modules);
        log.info("CrawlTaskDispatcher fans out to modules {}", modules);
    }

    @PostConstruct
    public void init() {
        jdbcClient.sql("""
            create table if not exists visit_requests (
                visit_id       varchar,
                domain_name    varchar(255) not null,
                dispatch_id    varchar(36),
                dispatched_at  timestamptz
            )
        """).update();

        jdbcClient.sql("""
            create table if not exists crawl_tasks (
                visit_id            varchar not null,
                domain_name         varchar(255) not null,
                crawler_module      varchar(100) not null,
                reservation_id      varchar(100),
                reserved_by         varchar(100),
                reserved_at         timestamptz,
                finished_at         timestamptz,
                status              varchar(100),
                attempts            int default 0
            )
        """).update();

        // Migration for databases created before finished_atp existed (the
        // "create table if not exists" above is a no-op there). Nullable, no default:
        // a metadata-only change in Postgres, idempotent, safe on every start.
        jdbcClient.sql("alter table crawl_tasks add column if not exists finished_at timestamptz").update();

        // DatabaseItemSource.acknowledge() looks rows up by exactly this key (up to batch-size ids
        // per call); without an index every ack would scan the whole table. Safe to drop on its own.
        jdbcClient.sql("create index if not exists idx_crawl_tasks_module_visit "
                + "on crawl_tasks (crawler_module, visit_id)").update();
        log.info("Queue tables verified");
    }

    /**
     * Runs the fan-out. Scheduled at {@code pipeline.queue.dispatch-interval}; also callable
     * directly (e.g. in tests). No-op when there are no undispatched visits.
     */
    @Scheduled(fixedDelayString = "${pipeline.queue.dispatch-interval}")
    @Transactional("queueTransactionManager")
    public void dispatch() {
        String token = UUID.randomUUID().toString();
        int stamped = jdbcClient.sql(STAMP_SQL).param("token", token).update();
        if (stamped == 0) {
            log.debug("dispatch(): no undispatched visits");
            return;
        }
        int inserted = jdbcClient.sql(fanOutSql).param("token", token).update();
        log.info("dispatch(): fanned {} visit(s) into {} crawl_tasks row(s) across {} module(s)",
                stamped, inserted, modules.size());
    }

    private static String buildFanOutSql(List<String> modules) {
        if (modules.isEmpty()) {
            throw new IllegalStateException("pipeline.queue.crawler-modules must not be empty");
        }
        String values = modules.stream()
                .map(CrawlTaskDispatcher::validate)
                .map(m -> "('" + m + "')")
                .collect(Collectors.joining(", "));
        return """
                insert into crawl_tasks (visit_id, domain_name, crawler_module, status)
                select vr.visit_id, vr.domain_name, m.module, 'PENDING'
                from   visit_requests vr
                cross join (values %s) as m(module)
                where  vr.dispatch_id = :token
                """.formatted(values);
    }

    private static String validate(String module) {
        if (module == null || !MODULE_NAME.matcher(module).matches()) {
            throw new IllegalArgumentException("Invalid crawler module name: '" + module
                    + "' (allowed: " + MODULE_NAME.pattern() + ")");
        }
        return module;
    }
}

