package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisit;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebCrawlResult;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebRepository;
import com.github.f4b6a3.ulid.Ulid;
import be.dnsbelgium.mercator.visits.CrawlerModule;
import be.dnsbelgium.mercator.visits.VisitResult;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static be.dnsbelgium.mercator.persistence.Repository.*;

@SuppressWarnings("SqlDialectInspection")
@Component
public class VisitRepository {

  private final JdbcTemplate jdbcTemplate;
  private final JdbcClient jdbcClient;
  private final TableCreator tableCreator;
  private final WebRepository webRepository;

  private final MeterRegistry meterRegistry;

  private static final Logger logger = LoggerFactory.getLogger(VisitRepository.class);

  private final static Duration WARN_AFTER = Duration.ofSeconds(5);

  private int databaseCounter = 0;
  private String databaseName;
  private File databaseFile;

  @Value("${visits.export.directory}")
  @Setter @Getter
  private File exportDirectory;

  @Value("${visits.database.directory}")
  @Setter @Getter
  private File databaseDirectory;

  @Value("${visits.database.deleteAfterExport:true}")
  boolean deleteDatabaseAfterExport;

  @Value("${visits.database.ulidInDatabaseName:true}")
  boolean ulidInDatabaseName;

  @Autowired
  public VisitRepository(DuckDataSource dataSource, TableCreator tableCreator, WebRepository webRepository, MeterRegistry meterRegistry) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.jdbcClient = JdbcClient.create(dataSource);
    this.tableCreator = tableCreator;
    this.webRepository = webRepository;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public void init() {
    makeDatabaseDirectory();
    makeExportDirectory();
    // TODO: we should check if a database still exists from a previous run
    logger.info("creating a new database");
    newDatabase();
  }

  @SneakyThrows
  private void makeDatabaseDirectory() {
    if (databaseDirectory == null) {
      databaseDirectory = new File(System.getProperty("user.home"));
      logger.warn("databaseDirectory not set => using {}", databaseDirectory);
    }
    logger.info("databaseDirectory: {}", databaseDirectory);
    FileUtils.forceMkdir(databaseDirectory);
  }

  @SneakyThrows
  private void makeExportDirectory() {
    if (exportDirectory == null) {
      exportDirectory = new File(System.getProperty("user.home"));
      logger.warn("exportDirectory not set => using {}", exportDirectory);
    }
    FileUtils.forceMkdir(exportDirectory);
  }

  @Transactional
  public List<String> getTableNames() {
    attachAndUse();
    return jdbcClient
        .sql("select table_name from information_schema.tables where table_type = 'BASE TABLE' and table_catalog = ? ")
        .param(databaseName)
        .query(String.class)
        .list();
  }

  @Transactional
  @Timed("save.visitResult")
  public void save(VisitResult visitResult) {
    var start = Instant.now();
    var dbName = databaseName;
    try {
      logDatabases();
      logger.debug("save:: now: {}", start);

      attachAndUse();

      // TODO: added ugly null checks for now, until we refactor towards CrawlerModule's

      if (visitResult.getDnsCrawlResult() != null) {
        save(visitResult.getDnsCrawlResult());
      }

      if (visitResult.getCollectedData() != null) {
        for (CrawlerModule<?> crawlerModule : visitResult.getCollectedData().keySet()) {
          List<?> data = visitResult.getCollectedData().get(crawlerModule);
          crawlerModule.save(data);
        }
      }

      var duration = Duration.between(start, Instant.now());
      logger.info("Done saving VisitResult for {}, took {}", visitResult.getVisitRequest(), duration);

    } catch (Exception e) {
      logger.info("save on {} started at {} failed", dbName, start);
      throw e;
    }
  }

  @Transactional
  public List<WebCrawlResult> findWebCrawlResults (String visitId) {
    attachAndUse();
    List<WebCrawlResult>  webCrawlResults = webRepository.findWebCrawlResult(visitId);
    // TODO: it's bit of a mess, discuss with team how to do this better
    for (WebCrawlResult webCrawlResult : webCrawlResults) {
      List<PageVisit> pageVisits = webRepository.findPageVisits(visitId);
      webCrawlResult.setPageVisits(pageVisits);
      List<HtmlFeatures> featuresList = webRepository.findHtmlFeatures(visitId);
      webCrawlResult.setHtmlFeatures(featuresList);
    }
    return webCrawlResults;
  }

  @Transactional
  public void exportDatabase(boolean attachNewDatabase) {
    exportDatabase();
    if (attachNewDatabase) {
      newDatabase();
      logTables();
      logDatabases();
    }
    logger.info("exportAndStartNewDatabase: searchPath = [{}]", searchPath());
  }

  public Map<String, Object> databaseSize() {
    Map<String, Object> map = jdbcTemplate.queryForMap(
            "select * from pragma_database_size() where database_name = ?", databaseName);
    logger.debug("map = {}", map);
    return map;
  }

  private void newDatabase() {
    this.databaseCounter++;
    // The databaseCounter will restart from zero when the process is restarted
    // Its only purpose is to make it easier to spot the most recently created database
    if (ulidInDatabaseName) {
      this.databaseName = "visits_db_" + databaseCounter + "_" + Ulid.fast();
    } else {
      this.databaseName = "visits_db_" + databaseCounter;
    }
    this.databaseFile = new File(databaseDirectory, databaseName + ".db");
    attachAndUse();
    tableCreator.createVisitTables();
    // log name of database in scheduler DB ?
    // logTables();
  }

  public void logTables() {
    var x = "show all tables";
    var tables = jdbcTemplate.queryForList("select database, schema, name from (" + x + ") ");
    for (var table : tables) {
      logger.debug("{}.{}.{}", table.get("database"), table.get("schema"), table.get("name"));
    }
  }

  public String searchPath() {
    var query = "SELECT current_setting('search_path')";
    return jdbcTemplate.queryForObject(query, String.class);
  }

  public Long transactionId() {
    var query = "SELECT txid_current()";
    return jdbcTemplate.queryForObject(query, Long.class);
  }

  private void exportDatabase() {
    String destinationDir = exportDirectory.getAbsolutePath() + File.separator + databaseName + File.separator;
    logger.info("destinationDir = {}", destinationDir);
    var transactionId = transactionId();
    logger.debug("before export: transactionId = {}", transactionId);
    executeStatement("use " + databaseName);
    executeStatement("checkpoint");
    String export = """
            export database '%s'
            (
                FORMAT PARQUET,
                COMPRESSION ZSTD,
                ROW_GROUP_SIZE 100_000
            )
            """.formatted(destinationDir);
    var duration = executeStatement(export);
    logger.info("Exporting to {} took {}", destinationDir, duration);
    logger.debug("after export: transactionId = {}", transactionId);
    executeStatement("ATTACH if not exists ':memory:' AS memory ");
    executeStatement("use memory ");
    executeStatement("DETACH " + databaseName);
    if (deleteDatabaseAfterExport) {
      deleteDatabaseFile();
    }
  }

  private void deleteDatabaseFile() {
    try {
      FileUtils.delete(databaseFile);
      logger.info("deleted {}", databaseFile);
    } catch (IOException e) {
      logger.atError()
              .setMessage("Could not delete database file {}")
              .addArgument(databaseFile)
              .setCause(e)
              .log();
    }
  }

  private Duration executeStatement(String sql) {
    var started = Instant.now();
    jdbcTemplate.execute(sql);
    var finished = Instant.now();
    var duration = Duration.between(started, finished);
    if (duration.compareTo(WARN_AFTER) > 0) {
      logger.warn("Statement took {} SQL: {}", duration, sql);
    }
    logger.debug("Done executing sql = {} took {}", sql, duration);
    //repository.saveOperation(started, sql, duration);
    return duration;
  }

  private void logDatabases() {
    var databases = jdbcTemplate.queryForList("show databases", String.class);
    for (var database : databases) {
      logger.debug("we have this database attached: '{}' ", database);
    }

  }

  public void attachAndUse() {
    var attach = String.format("ATTACH if not exists '%s' AS %s", databaseFile.getAbsolutePath(), databaseName);
    executeStatement(attach);
    executeStatement("use " + databaseName);
  }

  public static List<String> getList(ResultSet rs, String columnName) throws SQLException {
    var array = rs.getArray(columnName).getArray();
    return Arrays.stream((Object[]) array).map(Object::toString).toList();
  }


  public void save(@NotNull DnsCrawlResult crawlResult) {
    logger.info("Saving DnsCrawlResult with status {}", crawlResult.getStatus());
    Timer.Sample sample = Timer.start(meterRegistry);
    for (var req : crawlResult.getRequests()) {
      String id = Ulid.fast().toString();
      req.setId(id);
      insertDnsRequest(req);
      for (Response response : req.getResponses()) {
        insertResponse(req, response);
        for (ResponseGeoIp geoIp : response.getResponseGeoIps()) {
          // TODO: this happens when geo ip is disabled. Can we do it cleaner?
          if (geoIp != null) {
            insertGeoIp(response, geoIp);
          }
        }
      }
    }
    sample.stop(meterRegistry.timer("repository.save.dns.crawlresult"));
  }

  public void insertDnsRequest(@NotNull Request req) {
    Timer.Sample sample = Timer.start(meterRegistry);
    var insert = """
            insert into dns_request(
                id,
                visit_id,
                domain_name,
                prefix,
                record_type,
                rcode,
                ok,
                problem,
                num_of_responses,
                crawl_timestamp
            )
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    jdbcTemplate.update(
            insert,
            req.getId(),
            req.getVisitId(),
            req.getDomainName(),
            req.getPrefix(),
            req.getRecordType().toString(),
            req.getRcode(),
            req.isOk(),
            req.getProblem(),
            req.getNumOfResponses(),
            timestamp(req.getCrawlTimestamp())
    );
    sample.stop(meterRegistry.timer("repository.insert.dns.request"));
  }

  public void insertResponse(@NotNull Request request, @NotNull Response response) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String insert =
            """
                        insert into dns_response(id, dns_request, record_data, ttl)
                        values (?, ?, ?, ?)
                    """;
    String id = Ulid.fast().toString();
    response.setId(id);
    jdbcTemplate.update(
            insert,
            id,
            request.getId(),
            response.getRecordData(),
            response.getTtl()
    );
    sample.stop(meterRegistry.timer("repository.insert.dns.response"));
  }

  public void insertGeoIp(@NotNull Response response, @NotNull ResponseGeoIp responseGeoIp) {
    var sample = Timer.start(meterRegistry);
    String insert = """
            insert into response_geo_ips
            (dns_response, asn, country, ip, asn_organisation, ip_version)
            values (?, ?, ?, ?, ?, ?)
            """;
    jdbcTemplate.update(
            insert,
            response.getId(),
            responseGeoIp.getAsn(),
            responseGeoIp.getCountry(),
            responseGeoIp.getIp(),
            responseGeoIp.getAsnOrganisation(),
            responseGeoIp.getIpVersion()
    );
    sample.stop(meterRegistry.timer("repository.insert.dns.response.geo.ip"));
  }


}
