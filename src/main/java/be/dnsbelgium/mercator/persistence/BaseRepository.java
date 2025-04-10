package be.dnsbelgium.mercator.persistence;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@SuppressWarnings("SqlSourceToSinkFlow")
public class BaseRepository<T> {

  private static final Logger logger = LoggerFactory.getLogger(BaseRepository.class);

  @Setter
  @Getter
  public static class SearchVisitIdResultItem {
    private String visitId;
    private Instant timestamp;

    public SearchVisitIdResultItem(String visitId, Instant timestamp) {
      this.visitId = visitId;
      this.timestamp = timestamp;
    }

  }

  private final ObjectMapper objectMapper;

  private final String baseLocation;

  private final JdbcClient jdbcClient;

  private final Class<T> type;

  @SneakyThrows
  public BaseRepository(ObjectMapper objectMapper, String baseLocation, Class<T> type) {
    this.objectMapper = objectMapper;
    if (baseLocation == null || baseLocation.isEmpty()) {
      throw new IllegalArgumentException("baseLocation must not be null or empty");
    }
    logger.info("baseLocation = [{}]", baseLocation);
    this.baseLocation = createDestination(baseLocation);
    this.jdbcClient = JdbcClient.create(DuckDataSource.memory());
    this.type = type;
  }

  public static boolean isURL(String dataLocation) {
    try {
      return new URI(dataLocation).getScheme() != null;
    } catch (Exception e) {
      return false;
    }
  }

  public static String createDestination(String... parts) throws IOException {
    if (isURL(parts[0])) {
      return String.join("/", parts);
    } else {
      Path destPath = Path.of(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
      Files.createDirectories(destPath);
      return destPath.toAbsolutePath().toString();
    }
  }

  public String getAllItemsQuery() {
    return StringSubstitutor.replace("""
        select * exclude (year, month) from read_parquet('${base_location}/**/*.parquet')
        """, Map.of("base_location", baseLocation));
  }

  public String timestampField() {
    return "crawl_timestamp";
  }

  public void setVariables(JdbcClient jdbcClient) {
  }

  public String domainNameField() { return "domain_name"; }

  @SneakyThrows
  public List<SearchVisitIdResultItem> searchVisitIds(String domainName) {
    String query = StringSubstitutor.replace("""
        with all_items as (${get_all_items_query}),
        foo as (
          select visit_id, ${timestamp_field} as timestamp
          from all_items
          where ${domain_name_field}=:domainName
        )
        select row_to_json(foo)
        from foo
        """, Map.of("get_all_items_query", getAllItemsQuery(),
                    "timestamp_field", timestampField(),
                    "domain_name_field", domainNameField()
        ));
    return queryForList(query, domainName, SearchVisitIdResultItem.class);
  }

  @SneakyThrows
  public Optional<T> findByVisitId(String visitId) {
    String query = StringSubstitutor.replace("""
        with all_items as (${get_all_items_query})
        select row_to_json(all_items)
        from all_items
        where visit_id=:visit_id
        limit 1
        """, Map.of("get_all_items_query", getAllItemsQuery()));
    return queryForObject(Map.of("visit_id", visitId), query);
  }

  @NonNull
  @SneakyThrows
  private Optional<T> queryForObject(Map<String,?> params, String query) {
    try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
      JdbcClient jdbcClient = JdbcClient.create(dataSource);
      setVariables(jdbcClient);
      Optional<String> json = jdbcClient.sql(query)
              .params(params)
              .query(String.class)
              .optional();
      if (json.isPresent()) {
        try {
          T result = objectMapper.readValue(json.get(), this.type);
          logger.debug("Found: \n{}", result);
          return Optional.of(result);
        } catch (JsonMappingException e) {
          logger.error("JsonMappingException {} for \n {}", e.getMessage(), json);
          throw e;
        }
      }
      return Optional.empty();
    }
  }

  @SneakyThrows
  private <S> List<S> queryForList(String query, String domainName, Class<S> clazz) {
    try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
      JdbcClient jdbcClient = JdbcClient.create(dataSource);
      setVariables(jdbcClient);
      List<String> jsonList = jdbcClient.sql(query)
              .param("domainName", domainName)
              .query(String.class)
              .list();
      List<S> found = new ArrayList<>();
      for (String json : jsonList) {
        S result = objectMapper.readValue(json, clazz);
        found.add(result);
      }
      return found;
    }
  }

  @SneakyThrows
  public Optional<T> findLatestResult(String domainName) {
    String query = StringSubstitutor.replace("""
        with all_items as (${get_all_items_query})
        select row_to_json(all_items)
        from all_items
        where ${domain_name_field} = :domainName
        order by ${timestamp_field} desc
        limit 1
        """, Map.of("get_all_items_query", getAllItemsQuery(),
                    "timestamp_field", timestampField(),
                    "domain_name_field", domainNameField()));
    return queryForObject(Map.of("domainName", domainName), query);
  }

  @SneakyThrows
  public List<T> findByDomainName(String domainName) {
    String query = StringSubstitutor.replace("""
        with all_items as (${get_all_items_query})
        select row_to_json(all_items)
        from all_items
        where ${domain_name_field}=:domainName
        """, Map.of(
              "get_all_items_query", getAllItemsQuery(),
              "domain_name_field", domainNameField()));
    return queryForList(query, domainName, type);
  }

  /**
   * Stores jsonResultsLocation in the repository, i.e. exports from json to parquet (the format that the repo understands)
   *
   * @param jsonResultsLocation : location of JSON file(s)
   */
  public void storeResults(String jsonResultsLocation) {

    jdbcClient.sql(String.format("""
      copy (
        select
          *,
          year(to_timestamp("%s")) as year,
          month(to_timestamp("%s")) as month
        from read_json('%s')
      ) to '%s' (format parquet, partition_by (year, month), OVERWRITE_OR_IGNORE, filename_pattern 'data_{uuid}')""",
            timestampField(), timestampField(), jsonResultsLocation, baseLocation)
    ).update();
  }

  @SneakyThrows
  protected String readFromClasspath(String path) {
    Resource resource = new ClassPathResource(path);
    return resource.getContentAsString(StandardCharsets.UTF_8);

  }

  /*
   * This method will use the passed in CTE definitions and the name of a specific CTE to copy the data to the given destination
  */
  void copyToParquet(String jsonLocation, DataSource dataSource, String cteDefinitions, String cte, String destination) {
    String copyStatement = StringSubstitutor.replace("""
                COPY (
                    ${cteDefinitions}
                    select * from ${cte}
                ) TO '${destination}'
                  (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN '{uuid}')
                """, Map.of(
                    "cteDefinitions", cteDefinitions,
                    "cte", cte,
                    "destination", destination
            )
    );
    logger.debug("cte={}, copyStatement=\n{}", cte, copyStatement);
    JdbcClient jdbcClient = JdbcClient.create(dataSource);
    jdbcClient.sql("set variable jsonLocation = ?")
            .param(jsonLocation)
            .update();
    jdbcClient.sql(copyStatement).update();
    logger.info("copying {} as parquet to {} done", cte, destination);
  }
}
