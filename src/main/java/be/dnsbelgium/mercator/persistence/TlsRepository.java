package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

@Component
public class TlsRepository extends BaseRepository<TlsCrawlResult> {

  private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);

  private final String visitsLocation;

  private final String certificatesLocation;

  @SneakyThrows
  public TlsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}tls") String dataLocation) {
    super(objectMapper, dataLocation, TlsCrawlResult.class);
    this.visitsLocation = createDestination(dataLocation, "visits");
    this.certificatesLocation = createDestination(dataLocation, "certificates");
  }

  @Override
  public String getAllItemsQuery() {
    String allItemsQuery = readFromClasspath("sql/tls/get_all_items.sql");
    String query = StringSubstitutor.replace(allItemsQuery, Map.of(
            "base_location", this.visitsLocation
    ));
    logger.info("query: {}", query);
    return query;
  }

  @Override
  public void storeResults(String jsonLocation) {
    try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
      String cteDefinitions = readFromClasspath("sql/tls/cte_definitions.sql");
      logger.debug("cteDefinitions: {}", cteDefinitions);
      logger.debug("visitsLocation = {}", visitsLocation);
      logger.debug("certificatesLocation = {}", certificatesLocation);
      copyToParquet(jsonLocation, dataSource, cteDefinitions, "export_visits", visitsLocation);
      exportCertificates(jsonLocation, dataSource);
    }
  }

  private void exportCertificates(String jsonLocation, DataSource dataSource) {
    logger.info("certificatesLocation: {}", certificatesLocation);
    String destination = certificatesLocation;
    if (!destination.endsWith(".parquet")) {
      destination = destination + "/" + UUID.randomUUID() + ".parquet";
    }
    String cteDefinitions = readFromClasspath("sql/tls/cte_definitions.sql");
    String copyStatement = StringSubstitutor.replace("""
            COPY (
                ${cteDefinitions}
                SELECT * FROM export_certificates
            ) TO '${destination}' (FORMAT parquet)
            """,
            Map.of(
                    "cteDefinitions", cteDefinitions,
                    "destination", destination
            )
    );
    logger.debug("copyStatement=\n{}", copyStatement);
    JdbcClient jdbcClient = JdbcClient.create(dataSource);
    jdbcClient.sql("SET VARIABLE jsonLocation = ?")
            .param(jsonLocation)
            .update();
    jdbcClient.sql(copyStatement).update();
    logger.info("Copying certificates to {} in Parquet format done", certificatesLocation);
  }

}
