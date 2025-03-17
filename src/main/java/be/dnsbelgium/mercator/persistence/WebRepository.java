package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.ulid.Ulid;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("SqlSourceToSinkFlow")
@Component
public class WebRepository {

  private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);
  private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());

  private final Path webCrawlDestination;
  private final Path pageVisitDestination;
  private final Path featuresDestination;
  private final ObjectMapper objectMapper;

  private final static String QUERY = """
      with
          crawl_result as (select * exclude (year, month) from read_parquet('%s/**/*.parquet', union_by_name=True)),
          html_feature as (select * exclude (year, month) from read_parquet('%s/**/*.parquet', union_by_name=True)),
          page_visit   as (select * exclude (year, month) from read_parquet('%s/**/*.parquet', union_by_name=True)),
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
              %s
          )
      select row_to_json(combined)
      from combined
      %s
    """;

  public WebRepository(
          ObjectMapper objectMapper,
          @Value("${mercator.data.location:mercator/data/}") String dataLocation) {
    this.objectMapper = objectMapper;
    if (dataLocation == null || dataLocation.isEmpty()) {
      throw new IllegalArgumentException("dataLocation must not be null or empty");
    }
    logger.info("dataLocation = [{}]", dataLocation);
    webCrawlDestination = Path.of(dataLocation, "web", "crawl_result");
    pageVisitDestination = Path.of(dataLocation, "web", "page_visit");
    featuresDestination = Path.of(dataLocation, "web", "html_features");
  }

  public List<String> searchVisitIds(String domainName) {
    // TODO: add maxResults parameter
    logger.info("Searching visitIds for domainName={}", domainName);
    return List.of();
  }

  @SneakyThrows
  public Optional<WebCrawlResult> findLatestResult(String domainName) {
    logger.info("Finding latest crawl result for domainName={}", domainName);
    String query = QUERY.formatted(
            webCrawlDestination,
            featuresDestination,
            pageVisitDestination,
            "where crawl_result.domain_name = ?",
            " order by crawl_started desc limit 1"
    );
    Optional<String> json = jdbcClient.sql(query)
            .param(domainName)
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);
      logger.debug("Found: \n{}", webCrawlResult);
      return Optional.of(webCrawlResult);
    }
    return Optional.empty();
  }

  public Optional<WebCrawlResult> findByVisitId(String visitId) {
    // TODO
    logger.info("Finding latest crawl result for visitId={}", visitId);
    return Optional.empty();
  }


  /**
   * Converts the given JSON file to parquet format.
   */
  @SneakyThrows
  public void toParquet(Path jsonFile) {
    Files.createDirectories(webCrawlDestination);
    Files.createDirectories(pageVisitDestination);
    Files.createDirectories(featuresDestination);

    // generate a unique tableName since to avoid collisions with other threads
    String tableName = "webCrawlResult_" + Ulid.fast();
    String createTable = """
      create table %s
      as
      select *,
             to_timestamp(crawl_started) as ts,
             extract('year' from ts) as year,
             extract('month' from ts) as month,
      from read_json('%s')
      """.formatted(tableName, jsonFile);
    logger.info("createTable = \n{}", createTable);
    jdbcClient.sql(createTable).update();

    String copyWebCrawlResult = """
            copy (
                select * exclude (ts, html_features, page_visits)
                from %s
              )
            to '%s' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'web_{uuid}' )
            """.formatted(tableName, webCrawlDestination);
    logger.debug("copyWebCrawlResult: \n{}", copyWebCrawlResult);
    jdbcClient.sql(copyWebCrawlResult).update();

    String copyPageVisits = """
            copy (
              select unnest(page_visits, recursive:=True), year, month
              from %s
            )
            to '%s' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'page_visit_{uuid}' )
            """
            .formatted(tableName, pageVisitDestination);
    logger.info("copyPageVisits = \n{}", copyPageVisits);
    jdbcClient.sql(copyPageVisits).update();

    String copyHtmlFeatures = """
            copy (
              select unnest(html_features, recursive:=True), year, month
              from %s
            )
            to '%s' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'html_{uuid}' )
            """
            .formatted(tableName, featuresDestination);
    logger.info("copyHtmlFeatures = \n{}", copyHtmlFeatures);
    jdbcClient.sql(copyHtmlFeatures).update();
  }

  @SneakyThrows
  public List<WebCrawlResult> findByDomainName(String domainName) {
    // TODO: add limit parameter and order by crawl_timestamp desc
    String query = QUERY.formatted(
            webCrawlDestination, featuresDestination, pageVisitDestination, "where crawl_result.domain_name = ?", "");
    logger.info("query = {}", query);
    List<String> jsonList = jdbcClient.sql(query)
            .param(domainName)
            .query(String.class)
            .list();
    List<WebCrawlResult> found = new ArrayList<>();
    for (String json : jsonList) {
      WebCrawlResult webCrawlResult = objectMapper.readValue(json, WebCrawlResult.class);
      found.add(webCrawlResult);
    }
    return found;
  }



}
