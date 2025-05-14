package be.dnsbelgium.mercator.persistence;

import lombok.SneakyThrows;
import org.duckdb.DuckDBConnection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

public class DuckDataSource extends AbstractDriverBasedDataSource implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DuckDataSource.class);

    private DuckDBConnection connection;

    @SneakyThrows
    public void init() {
        String url = getUrl();
        logger.info("Creating connection with url = {}", url);
        Objects.requireNonNull(url);
        // TODO: should we only execute this when e.g. env var is set with init script?
        this.connection = (DuckDBConnection) DriverManager.getConnection(url);
        // create s3 secret
        try (Statement stmt = connection.createStatement()) {
            String create_secret = "CREATE OR REPLACE SECRET (TYPE S3, PROVIDER CREDENTIAL_CHAIN)";
            logger.info("executing: {}", create_secret);
            stmt.executeQuery(create_secret);
            logger.info("s3 secret created");
        }
    }

    public DuckDataSource(String url) {
        setUrl(url);
        init();
    }

    public static DuckDataSource memory() {
        return new DuckDataSource("jdbc:duckdb:");
    }

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
        return connection.duplicate();
    }

    public DuckDBConnection connection() {
        return connection;

    }



}
