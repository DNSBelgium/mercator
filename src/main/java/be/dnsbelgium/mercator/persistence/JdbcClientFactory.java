package be.dnsbelgium.mercator.persistence;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Component
public class JdbcClientFactory {

  @Value("${duckdb.create.s3.secret:false}")
  private boolean createS3Secret;

  @Value("${duckdb.log.s3.secrets:false}")
  private boolean logSecrets;

  private static final Logger logger = LoggerFactory.getLogger(JdbcClientFactory.class);

  public JdbcClient jdbcClient() {
    String url = "jdbc:duckdb:";
    DataSource dataSource = new SingleConnectionDataSource(url, true);
    logger.info("created dataSource with url='{}'", url);
    JdbcClient jdbcClient = JdbcClient.create(dataSource);
    logSecrets(jdbcClient);
    if (createS3Secret) {
      createSecret(dataSource);
    }
    if (logSecrets) {
      logSecrets(jdbcClient);
    }
    return jdbcClient;
  }

  @SneakyThrows
  private void createSecret(DataSource dataSource) {
    try (Statement stmt = dataSource.getConnection().createStatement()) {
      String create_secret = "CREATE OR REPLACE PERSISTENT SECRET (TYPE S3, PROVIDER CREDENTIAL_CHAIN)";
      logger.info("executing: {}", create_secret);
      stmt.executeQuery(create_secret);
      logger.info("s3 secret created");
    }
  }

  private void logSecrets(JdbcClient jdbcClient) {
    try {
      List<Map<String, Object>> secrets = jdbcClient.sql("from duckdb_secrets()").query().listOfRows();
      logger.info("DuckDB now knows about {} secrets", secrets.size());
      for (Map<String, Object> secret : secrets) {
        logger.info("* DuckDb knows secret: {}", secret);
      }
    } catch (Exception e) {
      logger.error("Exception while reading secrets: {}", e.getMessage());
    }

  }

}
