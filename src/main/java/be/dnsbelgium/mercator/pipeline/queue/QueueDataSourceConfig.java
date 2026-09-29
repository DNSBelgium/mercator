package be.dnsbelgium.mercator.pipeline.queue;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Dedicated Postgres {@link DataSource} + {@link JdbcClient} for the stateful work queue
 * (dispatch, lease, ack, reap). Active only under the {@code postgres-queue} profile; the
 * primary DuckDB DataSource (Parquet roll-up) is left untouched.
 *
 * <p>Connection settings come from the standard libpq environment variables
 * ({@code PGHOST}, {@code PGPORT}, {@code PGDATABASE}, {@code PGUSER}, {@code PGPASSWORD}),
 * so pointing the queue at Postgres or GizmoSQL is a pure environment change.
 * The {@code queueJdbcClient} bean is what {@code DatabaseItemSource} / the dispatcher /
 * the reaper inject via {@code @Qualifier("queueJdbcClient")}.
 *
 * <p>{@link EnableScheduling} lives here so the {@code @Scheduled} dispatcher/reaper only
 * run under this profile.
 */
@Slf4j
@Configuration
@Profile("postgres-queue")
@EnableScheduling
public class QueueDataSourceConfig {

    @Bean(name = "queueDataSource", destroyMethod = "close")
    public HikariDataSource queueDataSource() {
        String host = env("PGHOST", "localhost");
        String port = env("PGPORT", "5432");
        String database = requireEnv("PGDATABASE");
        String user = requireEnv("PGUSER");
        String password = requireEnv("PGPASSWORD");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        log.info("Creating queue DataSource for {} (user={})", url, user);

        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(user)
                .password(password)
                .build();
    }

    @Bean(name = "queueJdbcClient")
    public JdbcClient queueJdbcClient(@Qualifier("queueDataSource") DataSource queueDataSource) {
        log.info("creating the queueJdbcClient (Postgres work queue)");
        return JdbcClient.create(queueDataSource);
    }

    /**
     * Transaction manager bound to the queue DataSource, referenced by name from
     * {@code @Transactional("queueTransactionManager")} so the dispatcher's two-statement
     * fan-out commits (or rolls back) atomically without touching the primary DuckDB TM.
     */
    @Bean(name = "queueTransactionManager")
    public PlatformTransactionManager queueTransactionManager(
            @Qualifier("queueDataSource") DataSource queueDataSource) {
        return new DataSourceTransactionManager(queueDataSource);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable [" + name + "] is not set. "
                    + "Either set the PG* env variables or disable the 'postgres-queue' Spring profile.");
        }
        return value;
    }
}

