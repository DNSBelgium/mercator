package be.dnsbelgium.mercator.persistence;

import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.duckdb.DuckDBConnection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class DuckDataSource extends AbstractDriverBasedDataSource {

    private static final Logger logger = LoggerFactory.getLogger(DuckDataSource.class);

    private DuckDBConnection connection;

    @SneakyThrows
    //@PostConstruct
    public void init() {
        String url = getUrl();
        logger.info("Creating connection with url = {}", url);
        Objects.requireNonNull(url);
        this.connection = (DuckDBConnection) DriverManager.getConnection(url);
    }

    public DuckDataSource() {
    }

    public DuckDataSource(String url) {
        setUrl(url);
        init();
    }

    public static DuckDataSource memory() {
        return new DuckDataSource("jdbc:duckdb:");
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.info("Could not close DuckDBConnection = {}", e.getMessage());
            }
        }
    }

    @NotNull
    @Override
    protected Connection getConnectionFromDriver(@NotNull Properties props) throws SQLException {
        logger.debug("getConnectionFromDriver with props = {}", props);
        //return connection.duplicate();

        return new DelegatingConnection<>(connection.duplicate()) {
            @Override
            public void setTransactionIsolation(int level) throws SQLException {
                logger.info("setTransactionIsolation = {}", level);
                //super.setTransactionIsolation(level);
            }
        };

    }

    public DuckDBConnection connection() {
        return connection;

    }



}
