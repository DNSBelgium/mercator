package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Component
public class TlsRepository {

  // TODO: move to separate class
  private final String dataLocation;
  private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);

  private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());


  public TlsRepository(@Value("${mercator.data.location:mercator/data/}") String dataLocation) {
    if (dataLocation == null || dataLocation.isEmpty()) {
      throw new IllegalArgumentException("dataLocation must not be null or empty");
    }
    if (dataLocation.endsWith("/")) {
      this.dataLocation = dataLocation;
    } else {
      this.dataLocation = dataLocation + "/";
    }
    logger.info("dataLocation = [{}]", dataLocation);

  }

  public List<String> searchVisitIds(String domainName) {
    // TODO
    logger.info("Searching visitIds for domainName={}", domainName);
    return List.of();
  }

  public Optional<TlsCrawlResult> findLatestResult(String domainName) {
    // TODO
    logger.info("Finding latest crawl result for domainName={}", domainName);
    return Optional.empty();
  }

  public Optional<TlsCrawlResult> findByVisitId(String visitId) {
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    Path parquetFilePath = Path.of("test");

    // TODO: use location given to the repository to search instead of hardcoded
    String query = String.format("select to_json(p) from '%s' p where visit_id = ?", parquetFilePath);
      return jdbcClient
              .sql(query)
              .param(visitId)
              .query(TlsCrawlResult.class)
              .optional();
  }

  public void saveToParquet(Path jsonFile, String parquetFileName) {
    // TODO: partitioned
    String path = dataLocation + File.separator + parquetFileName;
    // copy json to parquet
    // rename fields
    String query = String.format("COPY (FROM '%s') TO '%s' (FORMAT parquet)", jsonFile, path);
    // partitioned
      //noinspection SqlSourceToSinkFlow
      jdbcClient
            .sql(query)
            .update();
    logger.info("Saved to parquet file at {}", path);


  }

}
