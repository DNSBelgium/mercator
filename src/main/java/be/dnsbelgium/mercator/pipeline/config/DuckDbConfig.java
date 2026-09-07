package be.dnsbelgium.mercator.pipeline.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DuckDbConfig {

    /**
     * The primary DataSource (DuckDB, used for the Parquet roll-up in JsonItemWriter).
     * Declared explicitly and marked {@link Primary} so that adding the secondary Postgres
     * {@code queueDataSource} (postgres-queue profile) does not make Spring Boot's
     * auto-configured DataSource / JdbcTemplate back off or become ambiguous.
     */
    @Bean
    @Primary
    DataSource dataSource(@Value("${spring.datasource.url}") String url,
                          @Value("${spring.datasource.driver-class-name:}") String driverClassName) {
        log.info("creating primary DuckDB DataSource for {}", url);
        DataSourceBuilder<?> builder = DataSourceBuilder.create().url(url);
        if (driverClassName != null && !driverClassName.isBlank()) {
            builder.driverClassName(driverClassName);
        }
        return builder.build();
    }

    @Bean
    @Primary
    JdbcClient jdbcClient(@Qualifier("dataSource") DataSource dataSource) {
        log.info("creating the primary (DuckDB) JdbcClient");
        return JdbcClient.create(new JdbcTemplate(dataSource));
    }

}
