package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebRepository;
import com.github.f4b6a3.ulid.Ulid;
import be.dnsbelgium.mercator.persistence.TableCreator;
import be.dnsbelgium.mercator.persistence.VisitRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static be.dnsbelgium.mercator.test.TestUtils.now;
import static org.assertj.core.api.Assertions.assertThat;

class VisitRepositoryTest {

  static DuckDataSource dataSource;
  static VisitRepository visitRepository;
  static JdbcClient jdbcClient;
  static MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @TempDir
  static File tempDir;

  private static final Logger logger = LoggerFactory.getLogger(VisitRepositoryTest.class);

  @BeforeAll
  public static void init() {
    dataSource = new DuckDataSource("jdbc:duckdb:");
    WebRepository webRepository = new WebRepository(dataSource, meterRegistry);
    TableCreator tableCreator = new TableCreator(dataSource, null, null, webRepository); // dees gaat wss failen door die nieuwe table, nog aanpassen.
    visitRepository = new VisitRepository(dataSource, tableCreator, webRepository, meterRegistry);
    visitRepository.setDatabaseDirectory(tempDir);
    visitRepository.setExportDirectory(tempDir);
    visitRepository.init();
    jdbcClient = JdbcClient.create(dataSource);
    logger.info("init done");
  }

//  @Test
//  void failOnPurpose() {
//    fail("tesing failure report");
//  }

  @Test
  @Transactional
  public void saveDnsCrawlResult() {
    Request request = new Request();
    request.setVisitId(VisitIdGenerator.generate());
    String requestId = Ulid.fast().toString();
    request.setId(requestId);
    request.setOk(true);
    request.setPrefix("www");
    request.setDomainName("google.be");
    request.setRecordType(RecordType.A);
    DnsCrawlResult crawlResult = DnsCrawlResult.of(List.of(request));
    visitRepository.save(crawlResult);
  }

  @Test
  public void insertResponse() {
    Request request = new Request();
    request.setVisitId(VisitIdGenerator.generate());
    String requestId = Ulid.fast().toString();
    String responseId = Ulid.fast().toString();
    request.setId(requestId);
    request.setOk(true);
    request.setPrefix("www");
    request.setDomainName("google.be");
    request.setRecordType(RecordType.A);
    Response response = new Response();
    response.setId(responseId);
    response.setTtl(3600L);
    response.setRecordData("IN 5 ns1.google.com");
    visitRepository.insertResponse(request, response);
  }

  @Test
  public void instantsInDuckdb() {
    Instant now = now();
    Timestamp timestamp = Timestamp.from(now);
    Instant instant = timestamp.toInstant();
    logger.info("now       = {}", now);
    logger.info("timestamp = {}", timestamp);
    logger.info("instant   = {}", instant);
    assertThat(instant).isEqualTo(now);
    DuckDataSource dataSource = new DuckDataSource("jdbc:duckdb:");
    JdbcClient jdbcClient = JdbcClient.create(dataSource);
    jdbcClient.sql("create table t1 (i timestamp)").update();
    jdbcClient
            .sql("insert into t1 (i) values (?)")
            .param(timestamp)
            .update();
    jdbcClient
            .sql("select * from t1")
            .query(rs -> {
              Timestamp ts = rs.getTimestamp("i");
              assertThat(ts).isEqualTo(timestamp);
              assertThat(ts.toInstant()).isEqualTo(instant);
            });
  }

}