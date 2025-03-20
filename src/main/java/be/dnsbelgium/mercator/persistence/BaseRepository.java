package be.dnsbelgium.mercator.persistence;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class BaseRepository<T> {

  private static final Logger logger = LoggerFactory.getLogger(BaseRepository.class);

  public static class SearchVisitIdResultItem {
    private String visitId;
    private Instant timestamp;

    public SearchVisitIdResultItem(String visitId, Instant timestamp) {
      this.visitId = visitId;
      this.timestamp = timestamp;
    }

    public String getVisitId() {
      return visitId;
    }

    public void setVisitId(String visitId) {
      this.visitId = visitId;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
      this.timestamp = timestamp;
    }
  }

  private final ObjectMapper objectMapper;

  private final String baseLocation;

  private final JdbcClient jdbcClient;

  private final Class<T> type;

  @SneakyThrows
  public BaseRepository(ObjectMapper objectMapper, String baseLocation, JdbcClient jdbcClient, Class<T> type) {
    this.objectMapper = objectMapper;
    if (baseLocation == null || baseLocation.isEmpty()) {
      throw new IllegalArgumentException("baseLocation must not be null or empty");
    }
    logger.info("baseLocation = [{}]", baseLocation);
    this.baseLocation = createDestination(baseLocation);
    this.jdbcClient = jdbcClient;
    this.type = type;
  }

  protected JdbcClient getJdbcClient() {
    return jdbcClient;
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

  public String domainNameField() { return "domain_name"; }

  @SneakyThrows
  public List<SearchVisitIdResultItem> searchVisitIds(String domainName) {
    String query = StringSubstitutor.replace("""
        with cte_all_items as (${get_all_items_query}),
        foo as (
          select visit_id, ${timestamp_field} as timestamp
          from cte_all_items 
          where ${domain_name_field}=?
        )
        select row_to_json(foo)
        from foo 
        """, Map.of("get_all_items_query", getAllItemsQuery(),
                    "timestamp_field", timestampField(),
                    "domain_name_field", domainNameField()
        ));

    List<String> jsonList = jdbcClient.sql(query)
        .param(domainName)
        .query(String.class)
        .list();
    List<SearchVisitIdResultItem> found = new ArrayList<>();
    for (String json : jsonList) {
      SearchVisitIdResultItem result = objectMapper.readValue(json, SearchVisitIdResultItem.class);
      found.add(result);
    }
    return found;
  }

  @SneakyThrows
  public Optional<T> findByVisitId(String visitId) { /* findByDomainNameQuery , order by timestamp field desc */
    String query = StringSubstitutor.replace("""
        with cte_all_items as (${get_all_items_query}) 
        select row_to_json(cte_all_items)
        from cte_all_items 
        where visit_id=?
        limit 1
        """, Map.of("get_all_items_query", getAllItemsQuery()));
    Optional<String> json = jdbcClient.sql(query)
        .param(visitId)
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

  @SneakyThrows
  public Optional<T> findLatestResult(String domainName) { /* findByDomainNameQuery, order by timestamp field desc, limit 1 */
    String query = StringSubstitutor.replace("""
        with cte_all_items as (${get_all_items_query}) 
        select row_to_json(cte_all_items)
        from cte_all_items 
        where ${domain_name_field}=?
        order by ${timestamp_field} desc
        limit 1
        """, Map.of("get_all_items_query", getAllItemsQuery(),
                    "timestamp_field", timestampField(),
                    "domain_name_field", domainNameField()));
    Optional<String> json = jdbcClient.sql(query)
        .param(domainName)
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

  @SneakyThrows
  public List<T> findByDomainName(String domainName) { /* getAllItemsQuery, add filter CTE to filter for visit id, limit 1 */
    String query = StringSubstitutor.replace("""
        with cte_all_items as (${get_all_items_query}) 
        select row_to_json(cte_all_items)
        from cte_all_items 
        where ${domain_name_field}=?
        """, Map.of("get_all_items_query", getAllItemsQuery(), "domain_name_field", domainNameField()));
    List<String> jsonList = jdbcClient.sql(query)
        .param(domainName)
        .query(String.class)
        .list();
    List<T> found = new ArrayList<>();
    for (String json : jsonList) {
      T result = objectMapper.readValue(json, this.type);
      found.add(result);
    }
    return found;
  }


  /**
   * Stores jsonResultsLocation in the repository, i.e. exports from json to parquet (the format that the repo understands)
   *
   * @param jsonResultsLocation
   */
  public void storeResults(String jsonResultsLocation) {

    jdbcClient.sql(String.format("""
      copy (
        select 
          *, 
          year(to_timestamp("%s")) as year, 
          month(to_timestamp("%s")) as month 
        from read_json('%s')
      ) to '%s' (format parquet, partition_by (year, month), filename_pattern 'data_{uuid}')""", timestampField(), timestampField(), jsonResultsLocation, baseLocation)).update();
  }
}
