package be.dnsbelgium.mercator.schedule;

import be.dnsbelgium.mercator.SimpleJobRunner;
import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.replace;

@Component
public class JobScheduler {

  @Value("${mercator.maxBatchSize:5000}")
  private int maxBatchSize;

  @Value("${mercator.uploadPath:/tmp/upload.csv}")
  private Path uploadPath;

  private final BatchConfig batchConfig;

  private final SimpleJobRunner simpleJobRunner;
  private final JdbcClient jdbcClient;
  private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

  public JobScheduler(SimpleJobRunner simpleJobRunner,
                      @Value("${mercator.database.file:mercator.db}") String databaseFile, BatchConfig batchConfig) {
    this.simpleJobRunner = simpleJobRunner;
    this.batchConfig = batchConfig;
    logger.info("Opening database file '{}'", databaseFile);
    String url = "jdbc:duckdb:" + databaseFile;
    logger.info("url = {}", url);
    DuckDataSource duckDataSource = new DuckDataSource(url);
    this.jdbcClient = JdbcClient.create(duckDataSource);
  }

  public Map<String, Object> stats() {
    String select = """
        select
           count(1) as queued,
           count(1) filter (where batch_id is not null) as in_progress,
           (select count(1) from done) as done,
        from queue
    """;
    Map<String, Object> stats = jdbcClient.sql(select).query().singleRow();
    logger.info("stats = {}", stats);
    return stats;
  }

  private String addUploadPath(String sql) {
    String newSql = StringUtils.replace(sql, "/tmp/upload.csv", uploadPath.toAbsolutePath().toString());
    logger.debug("newSql= {}", newSql);
    return newSql;
  }

  public int addToQueue(MultipartFile file) {
    try {
      logger.info("addToQueue with file of {} bytes", file.getSize());
      file.transferTo(new File(uploadPath.toAbsolutePath().toString()));
      String insert1 = addUploadPath("""
              insert into queue(visit_id, domain_name, priority, ingested_at)
              select
                coalesce(visit_id, uuidv7()::varchar) as visit_id,
                domain_name,
                coalesce(priority, 20) as priority,
                localtimestamp as ingested_at
               from '/tmp/upload.csv'
              """);
      int rowsInserted = tryInsert(insert1, false);
      if (rowsInserted < 0) {
        // try again without priority column
        String insert2 = addUploadPath("""
              insert into queue(visit_id, domain_name, priority, ingested_at)
              select
                coalesce(visit_id, uuidv7()::varchar) as visit_id,
                domain_name,
                20 as priority,
                localtimestamp as ingested_at
               from '/tmp/upload.csv'
              """);
        rowsInserted = tryInsert(insert2, true);
      }
      if (rowsInserted > 0) {
        logger.info("addToQueue: rowsInserted = {}", rowsInserted);
        return rowsInserted;
      } else {
        throw new RuntimeException("Uploaded file could not be processed");
      }
    } catch (IOException e) {
      logger.error("Failed to process file of {} bytes", file.getSize(), e);
      throw new RuntimeException(e);
    }
  }

  private int tryInsert(String insert, boolean throwOnError) {
    try {
     int rowsInserted = jdbcClient.sql(insert).update();
      logger.debug("tryInsert: rowsInserted = {}", rowsInserted);
      return rowsInserted;
  } catch (DataAccessException e) {
    if (throwOnError) {
      logger.error("Failed to execute \n {}", insert, e);
      logger.error("""
          Make sure you upload a CSV file with a header having at least these two columns:
          * visit_id: varchar (values can be null/empty)
          * domain_name: varchar (values can NOT be empty)
          Optional: priority: integer (values can be null/empty)
      """);
      throw new RuntimeException(e);
    } else {
      logger.info("Error {}", e.getMessage());
      // errorMessage probably already contains the insert statement, but let's log it again to be sure.
      logger.info("insert was \n{}", insert);
      return -1;
    }

  }

}

  private static VisitRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
    String visitId = rs.getString("visit_id");
    String domainName = rs.getString("domain_name");
    return new VisitRequest(visitId, domainName);
  }

  @PostConstruct
  public void init() {
     String create_table_queue = """
             create table if not exists queue (
               visit_id varchar,
               domain_name varchar,
               priority int,
               ingested_at timestamp,
               reserved_at timestamp,
               batch_id varchar
             )
     """;
     jdbcClient.sql(create_table_queue).update();
     logger.info("Created table: {}", create_table_queue);
     String create_table_done = """
             create table if not exists done (
               visit_id varchar,
               domain_name varchar,
               priority int,
               ingested_at timestamp,
               reserved_at timestamp,
               batch_id varchar,
               finished_at timestamp
             )
     """;
     jdbcClient.sql(create_table_done).update();
     logger.info("Created table: {}", create_table_done);
     logger.info("uploadPath = '{}'", uploadPath);
     logger.info("maxBatchSize = {}", maxBatchSize);
  }

  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
  public void startBatch() {
    String batchId = UlidCreator.getUlid().toString();
    logger.info("startBatch: check if there is work in the queue");
    int rowCount = createBatch(batchId);
    if (rowCount == 0) {
      logger.info("startBatch: no work found");
    } else {
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

  private void cleanUpAfterBatch() {
    Path outputPath = Paths.get(batchConfig.getOutputDirectory());
    boolean ok = FileSystemUtils.deleteRecursively(outputPath.toFile());
    logger.info("cleanUpAfterBatch: path={} ok = {}", outputPath, ok);
  }

  /**
   * We regularly reset batches that did nit finish on time to make sure the visits get processed.
   * This could lead to duplicate visits when the time to process a batch is longer than the expiry interval.
   * TODO: we could add a counter to the queue table and give up after X attempts.
   */
  @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
  public void resetExpiredBatches() {
    logger.info("resetExpiredBatches: starting");
    String reset = """
        update queue
        set reserved_at = null, batch_id = null
        where reserved_at < current_localtimestamp() - interval '4 hours'
    """;
    int rows = jdbcClient.sql(reset).update();
    logger.info("resetExpiredBatches: updated {} rows", rows);
  }

  // we don't need this method for now
  @SuppressWarnings("unused")
  public List<VisitRequest> retrieveBatch(String batchId) {
    String select = """ 
          select visit_id, domain_name
          from queue
          where batch_id = :batchId
          """;
    List<VisitRequest> visitRequests = jdbcClient
            .sql(select)
            .param("batchId", batchId)
            .query(JobScheduler::mapRow)
            .list();
    logger.info("Retrieved batch with {} visits", visitRequests.size());
    return visitRequests;
  }

  private void exportToCsv(String batchId) {
    // For now, we copy the visits of this batch to input.csv
    // because that is where the Spring Batch jobs expect it to be.
    // In a later version, we could adapt the jobs to read from the queue table.
    String exportToCsv = replace("""
              copy (
                    select visit_id, domain_name
                    from queue
                    where batch_id = :batchId
                    )
              to '/tmp/input.csv' (format csv, header false)
              """, "/tmp/input.csv", batchConfig.getInputFile()
    );
    int rows = jdbcClient
            .sql(exportToCsv)
            .param("batchId", batchId)
            .update();
    logger.info("exportToCsv: rows exported: {}", rows);
  }

  private int createBatch(String batchId) {
    String createBatch = """
      update queue
      set batch_id = :batchId,
          reserved_at = epoch_ms(:reserved_at)
      where visit_id
       in (
          select visit_id
          from queue
          where batch_id is null and reserved_at is null
          order by priority
          limit :maxBatchSize
          )
    """;
    int rowCount = jdbcClient
            .sql(createBatch)
            .param("batchId", batchId)
            .param("reserved_at", Instant.now().toEpochMilli())
            .param("maxBatchSize", maxBatchSize)
            .update();
    logger.info("createBatch: rowCount = {}", rowCount);
    return rowCount;
  }
  
  private void copyToDone(String batchId) {
    String copyToDone = """
             insert into done(
                 visit_id,
                 domain_name,
                 priority,
                 ingested_at,
                 reserved_at,
                 batch_id,
                 finished_at
            )
            select
                   visit_id,
                   domain_name,
                   priority,
                   ingested_at,
                   reserved_at,
                   batch_id,
                   localtimestamp
            from queue
            where batch_id = :batchId
            """;
    int rows = jdbcClient
            .sql(copyToDone)
            .param("batchId", batchId)
            .update();
    logger.info("copyToDone: copied {} rows to done for batchId = {}", rows, batchId);
  }

  private void deleteFromQueue(String batchId) {
    String deleteFromQueue = "delete from queue where batch_id = :batchId";
    int rows = jdbcClient
            .sql(deleteFromQueue)
            .param("batchId", batchId)
            .update();
    logger.info("deleted {} rows from queue for batchId = {}", rows, batchId);
  }

}
