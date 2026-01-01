package be.dnsbelgium.mercator.schedule;

import be.dnsbelgium.mercator.persistence.DuckDataSource;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings({"SqlResolve"})
@Component
public class LogShipping {

  private static final Logger logger = LoggerFactory.getLogger(LogShipping.class);

  private final LogShippingSettings settings;
  private final DuckDataSource dataSource;
  private final JdbcClient jdbcClient;
  private final String copyStatement;

  private boolean enabled;
  private int failureCount = 0;

  public LogShipping(LogShippingSettings settings) {
    this.settings = settings;
    logger.info("settings = {}", settings);
    this.dataSource = DuckDataSource.memory();
    this.jdbcClient = JdbcClient.create(dataSource);
    enabled = settings.isEnabled();
    logger.info("LogShipping enabled = {}", enabled);
    this.copyStatement = copyStatement();
    if (enabled) {
      logger.info("COPY stmt = \n {}", copyStatement);
    }
  }

  @Scheduled(fixedDelayString = "${log.shipping.interval:60s}")
  public void shipLogsInTransaction() {
    if (settings.isEnabled()) {
      DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
      transactionTemplate.executeWithoutResult(_ -> shipLogs());
    }
  }

  @SuppressWarnings("SameParameterValue")
  private String silenceIntelliJAboutSqlDialect(String sql) {
    return sql;
  }

  private void createSecret() {
    String create = silenceIntelliJAboutSqlDialect("""
        CREATE OR REPLACE SECRET my_secret (
          TYPE s3,
          PROVIDER config,
          KEY_ID :key_id,
          SECRET :secret,
          REGION :region
        )""");
    jdbcClient
        .sql(create)
        .param("key_id", settings.s3().getAccessKey())
        .param("secret", settings.s3().getSecretKey())
        .param("region", settings.s3().getRegion())
        .query(String.class)
        .single();
    logger.info("Created secret for region {}", settings.s3().getRegion());
  }

  public void setEndpoint() {
    jdbcClient.sql("set s3_endpoint = :s3_endpoint")
        .param("s3_endpoint", settings.s3().getEndpoint())
        .update();
    logger.info("Set s3_endpoint to {}", settings.s3().getEndpoint());
  }

  public void setRegion() {
    jdbcClient.sql("set s3_region = :s3_region")
        .param("s3_region", settings.s3().getRegion())
        .update();
    logger.info("Set s3_region to {}", settings.s3().getRegion());
  }

  public void shipLogs() {
    createSecret();
    setEndpoint();
    setRegion();
    copyLogEvents();
  }

  private void logS3Settings() {
    jdbcClient.sql("""
      select name || ':' || value
      from duckdb_settings()
      where name like 's3_%'
      and name not like '%secret%'
      """)
        .query(String.class)
        .list()
        .forEach(logger::info);
  }

  List<String> findFiles() {
    return jdbcClient
        .sql("select * from glob(:glob_pattern)")
        .param("glob_pattern", settings.getGlob_pattern())
        .query(String.class)
        .list();
  }

  public void deleteFiles(List<String> files) {
    for (String file : files) {
      try {
        Files.deleteIfExists(Path.of(file));
        logger.info("Deleted file = {}", file);
      } catch (IOException e) {
        logger.error("Error deleting file {} => disabling log shipping.", file, e);
        // immediately disable log shipping to avoid too many duplicates
        logger.warn("==================================================================");
        logger.warn("=== Log shipping disabled because we could not delete a file. ====");
        logger.warn("==================================================================");
        enabled = false;
      }
    }
  }

  private void copyLogEvents() {
    var files = findFiles();
    try {
      int rows = jdbcClient
          .sql(copyStatement)
          .update();
      logger.info("copied {} rows to s3://{}/", rows, settings.s3().getBucketName());
      deleteFiles(files);
    } catch (DataAccessException e) {
      if (e.getMessage().contains("No files found that match the pattern")) {
        logger.info("Found no files to process.");
      } else {
        logger.error("Error copying logs to s3", e);
        logS3Settings();
        processError();
      }
    }
  }

  private void processError() {
    failureCount++;
    if (settings.getMaxFailures() >= 0 && failureCount > settings.getMaxFailures()) {
      logger.error("log.shipping.max-failures={} but we have seen {} failures => disabling log shipping.", settings.getMaxFailures(), failureCount);
      logger.warn("==================================================================");
      logger.warn("=== Log shipping disabled after {} failures                   ====", failureCount);
      logger.warn("==================================================================");
      enabled = false;
    }
  }

  private @NonNull String copyStatement() {
    String copy_to_s3 = """
      copy (
          with
          log_events AS (
                  select
                    to_timestamp(timestamp) as ts,
                    _process_pid            as pid,
                    _process_thread_name    as thread,
                    _level_name             as level_name,
                    _log_logger             as logger,
                    _error_type             as error_type,
                    _error_stack_trace      as stack_trace,
                    _error_message          as error_message,
                    *
                    exclude (
                        timestamp,
                        _process_pid,
                        _process_thread_name,
                        _level_name,
                        _log_logger,
                        _error_message,
                        _error_type,
                        _error_stack_trace,
                        version
                    )
                  from read_json('{glob_pattern}', union_by_name=true, ignore_errors=true, filename=true)
          ),
          log_events_2  as (
              select year(timestamp) as year, month(timestamp) as month, day(timestamp) as day, *
              from log_events
          )
          from log_events_2
        )
        to 's3://{bucket_name}/app=mercator/'
          (FORMAT parquet, PARTITION_BY (year, month, day), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'logs-{{uuid}}')
    """;
    return copy_to_s3
        .replace("{glob_pattern}", settings.getGlob_pattern())
        .replace("{bucket_name}", settings.s3().getBucketName());
  }

}
