package be.dnsbelgium.mercator.schedule;

import be.dnsbelgium.mercator.SimpleJobRunner;
import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import com.github.f4b6a3.ulid.UlidCreator;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("SqlResolve")
@Component
@Profile( "postgres-queue")
public class JobSchedulerPostgres {

  private final int maxBatchSize;

  private final static String ATTACH_POSTGRES = avoidIntelliJWarning("ATTACH IF NOT EXISTS '' AS postgres_db (TYPE postgres)");

  private final SimpleJobRunner simpleJobRunner;
  private final JdbcClient jdbcClient;
  private final BatchConfig batchConfig;

  private static final Logger logger = LoggerFactory.getLogger(JobSchedulerPostgres.class);

  public JobSchedulerPostgres(
      SimpleJobRunner simpleJobRunner,
      BatchConfig batchConfig,
      @Value("${mercator.maxBatchSize:5000}") int maxBatchSize ) {
    this.simpleJobRunner = simpleJobRunner;
    this.maxBatchSize = maxBatchSize;
    this.batchConfig = batchConfig;
    DuckDataSource duckDataSource = DuckDataSource.memory();
    this.jdbcClient = JdbcClient.create(duckDataSource);
    logConfig();
    checkConfig();
  }

  public void logConfig() {
    logger.info("batchConfig = {}", batchConfig);
    logger.info("maxBatchSize = {}", maxBatchSize);
    logEnvironmentVariable("PGHOST");
    logEnvironmentVariable("PGPORT");
    logEnvironmentVariable("PGDATABASE");
    logEnvironmentVariable("PGUSER");
    logEnvironmentVariable("PGPASSWORD");
  }

  private void checkConfig() {
    assertEnvVariable("PGHOST");
    assertEnvVariable("PGDATABASE");
    assertEnvVariable("PGUSER");
    assertEnvVariable("PGPASSWORD");
  }

  private void assertEnvVariable(String name) {
    String value = System.getenv(name);
    if (value == null) {
      throw new IllegalStateException("Environment variable [" + name + "] is not set. Either set the PG env variables or disable Spring profile postgres-queue");
    }
  }

  private void logEnvironmentVariable(String name) {
    String value = System.getenv(name);
    if (value != null) {
      if (name.toUpperCase().contains("PASS")) {
        logger.info("Environment variable {} is set", name);
      } else {
        logger.info("Environment variable {} is set to '{}'", name, value);
      }
    } else {
      logger.info("Environment variable {} is not set", name);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static String avoidIntelliJWarning(String s) {
    return s;
  }

  // Every 5 seconds we check if there is any work in the queue.
  // And we start a batch if there is.
  @SuppressWarnings("DuplicatedCode")
  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
  public void startBatch() {
    String batchId = UlidCreator.getUlid().toString();
    logger.info("startBatch: check if there is work in the queue");
    attachPostgres();
    int rowCount = createBatch(batchId);
    if (rowCount == 0) {
      logger.info("startBatch: no work found");
    } else {
      logger.info("batch created with {} rows", rowCount);
      try {
        exportToCsv(batchId);
        simpleJobRunner.run();
        logger.info("simpleJobRunner.run() is done for batchId = {}", batchId);
        copyToDone(batchId);
        deleteFromQueue(batchId);
        cleanUpAfterBatch();
        logger.info("startBatch is done for batchId = {}", batchId);
      } catch (Exception e) {
        logger.error("Failed to run simpleJobRunner", e);
      }
    }
  }

  private void attachPostgres() {
    jdbcClient.sql(ATTACH_POSTGRES).update();
    logger.info("Postgres database attached");
  }

  private int createBatch(String batchId) {
    String createBatch = """
        update postgres_db.queue
        set batch_id = :batchId,
            reserved_at = now()
        where visit_id
              in (
                  select visit_id
                  from postgres_db.queue
                  where batch_id is null and reserved_at is null
                  order by priority
                  limit :maxBatchSize
              );
    """;
    logger.debug("createBatch = \n {}", createBatch);
    logger.debug("maxBatchSize = {}", maxBatchSize);
    Instant start = Instant.now();
    int rowCount = jdbcClient
        .sql(createBatch)
        .param("batchId", batchId)
        .param("maxBatchSize", maxBatchSize)
        .update();
    Instant done = Instant.now();
    Duration duration = Duration.between(start, done);
    logger.info("createBatch: rowCount = {}, seconds={} duration={}", rowCount, duration.getSeconds(), duration);
    return rowCount;
  }

  @SuppressWarnings("SqlSourceToSinkFlow")
  private void exportToCsv(String batchId) {
    // For now, we copy the visits of this batch to input.csv
    // because that is where the Spring Batch jobs expect it to be.
    // In a later version, we could adapt the jobs to read from the queue table.

    String copy = """
        copy (
              select visit_id, domain_name
              from postgres_db.queue
              where batch_id = :batchId
              )
        to '/tmp/inputFile' (format csv, header false)
        """;
    logger.info("batchConfig.getInputFile(): {}", batchConfig.getInputFile());

    String exportToCsv = Strings.CS.replace(copy,
        "/tmp/inputFile", batchConfig.getInputFile()
    );

    int rows = jdbcClient
        .sql(exportToCsv)
        .param("batchId", batchId)
        .update();
    logger.info("exportToCsv: rows exported: {}", rows);
  }

  private void copyToDone(String batchId) {
    String copyToDone = """
          insert into postgres_db.done(
                 visit_id, monthly_crawl_id, domain_name, priority, batch_id, reserved_at, finished_at, date_created, last_updated)
          select visit_id, monthly_crawl_id, domain_name, priority, batch_id, reserved_at, now(), now(), now()
          from postgres_db.queue
          where batch_id = :batchId
          """;
    int rows = jdbcClient
        .sql(copyToDone)
        .param("batchId", batchId)
        .update();
    logger.info("copyToDone: copied {} rows to done for batchId = {}", rows, batchId);
  }

  private void deleteFromQueue(String batchId) {
    String deleteFromQueue = "delete from postgres_db.queue where batch_id = :batchId";
    int rows = jdbcClient
        .sql(deleteFromQueue)
        .param("batchId", batchId)
        .update();
    logger.info("deleted {} rows from queue for batchId = {}", rows, batchId);
  }

  private void cleanUpAfterBatch() {
    File outputDirectory = Paths.get(batchConfig.getOutputDirectory()).toFile();
    boolean ok = FileSystemUtils.deleteRecursively(outputDirectory);
    logger.info("cleanUpAfterBatch: outputDirectory={} ok = {}", outputDirectory, ok);
  }

}
