package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SuppressWarnings("SqlSourceToSinkFlow")
class WebRepositoryTest {

  private static final Logger logger = LoggerFactory.getLogger(WebRepositoryTest.class);

  @TempDir
  static Path tempDir;
  static {
    if (System.getProperty("mercator_temp_dir") != null) {
      // this allows to run the tests with a folder that does not disappear after the test completes.
      tempDir = Path.of(System.getProperty("mercator_temp_dir"), UUID.randomUUID().toString());
      logger.info("Using temp dir {}", tempDir);
    }
  }
  private final ObjectMother objectMother = new ObjectMother();
  private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
  //private final WebRepository repository = new WebRepository(TestUtils.jsonReader(), tempDir.toAbsolutePath().toString());
  private final WebRepository repository = new WebRepository(TestUtils.jsonReader(), "/Users/maartenb/mercator/c6cbc17c-df26-4240-9355-cc771b6b703b");

  @Test
  public void toParquet() throws IOException {
    logger.info("tempDir = {}", tempDir);
    Files.createDirectories(tempDir);
    WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
    WebCrawlResult webCrawlResult2 = objectMother.webCrawlResult2();

    File jsonFile = tempDir.resolve("webCrawlResult1.json").toFile();
    logger.info("jsonFile = {}", jsonFile);

    ObjectWriter jsonWriter = TestUtils.jsonWriter();
    jsonWriter.writeValue(jsonFile, List.of(webCrawlResult1, webCrawlResult2));

    repository.toParquet(jsonFile.toPath());
    // todo: add asserts
    String sql = "select * from '%s/web/html_features/**/*.parquet'".formatted(tempDir.toString());
    var rows = jdbcClient.sql(sql).query().listOfRows();
    logger.info("rows = {}", rows);
    logger.info("rows.size = {}", rows.size());
    String to_json_sql = "select row_to_json(a) as json from '%s/web/html_features/**/*.parquet' a".formatted(tempDir.toString());
    rows = jdbcClient.sql(to_json_sql).query().listOfRows();
    logger.info("rows = {}", rows);
    logger.info("rows.size = {}", rows.size());
  }

  @Test
  public void find() throws IOException {
    String q2 = """
      with
          html_feature as (select * exclude (year, month) from '/Users/maartenb/mercator/c6cbc17c-df26-4240-9355-cc771b6b703b/web/html_features/**/*.parquet'),
          crawl_result as (select * exclude (year, month) from '/Users/maartenb/mercator/c6cbc17c-df26-4240-9355-cc771b6b703b/web/crawl_result/**/*.parquet'),
          page_visit   as (select * exclude (year, month) from '/Users/maartenb/mercator/c6cbc17c-df26-4240-9355-cc771b6b703b/web/page_visit/**/*.parquet'),
          features_per_visit as (
             select visit_id,
                    list(html_feature order by crawl_timestamp) as html_features
             from html_feature
             group by visit_id
          ),
          pages_per_visit as (
             select visit_id,
                    list(page_visit order by crawl_started) as page_visits
             from page_visit
             group by visit_id
          ),
          combined as (
              select
                 crawl_result.*,
                 coalesce(features_per_visit.html_features, []) as html_features,
                 coalesce(pages_per_visit.page_visits, []) as page_visits
              from crawl_result
              left join features_per_visit on crawl_result.visit_id = features_per_visit.visit_id
              left join pages_per_visit    on crawl_result.visit_id = pages_per_visit.visit_id
          )
      select row_to_json(combined)
      from combined
      order by visit_id
    """;
    List<String> json = jdbcClient.sql(q2).query(String.class).list();
    String json1 = json.get(0);
    String json2 = json.get(1);

    ObjectMapper mapper = TestUtils.jsonReader();

    WebCrawlResult wc1 = mapper.readValue(json1, WebCrawlResult.class);
    WebCrawlResult wc2 = mapper.readValue(json2, WebCrawlResult.class);

    assertThat(wc1).usingRecursiveComparison().isEqualTo(objectMother.webCrawlResult1());
    assertThat(wc2).usingRecursiveComparison().isEqualTo(objectMother.webCrawlResult2());
    System.out.println(Instant.now());
  }


  @Test
  public void find2() {
    List<WebCrawlResult> found = repository.find("dnsbelgium.be");
    logger.info("found = {}", found);
    logger.info("found {} WebCrawlResults", found.size());
  }

}