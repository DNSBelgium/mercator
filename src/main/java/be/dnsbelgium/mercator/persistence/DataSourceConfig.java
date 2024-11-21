package be.dnsbelgium.mercator.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${duckdb.datasource.url:jdbc:duckdb:monocator.duckdb}")
    private String url;

    @Bean
    public DuckDataSource duckDataSource() {
        logger.info("creating DuckDataSource using url=[{}]", url);
        DuckDataSource duckDataSource = new DuckDataSource();
        duckDataSource.setUrl(url);
        duckDataSource.init();
        return duckDataSource;
    }

    @Bean
    public JdbcTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

}
