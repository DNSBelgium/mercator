package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

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
    this.dataLocation = dataLocation;
  }

  public List<String> searchVisitIds(String domainName) {
    // TODO
    return List.of();
  }

  public Optional<TlsCrawlResult> findLatestCrawlResult(String domainName) {
    // TODO
    return Optional.empty();
  }

  public Optional<TlsCrawlResult> findByVisitId(String visitId) {
    // TODO
    return Optional.empty();
  }

  public void saveToParquet(Path jsonFile, String parquetFileName) {
    // TODO
    // bepaal pad adhv parquetFileName
    // copy json to parquet
    // rename velden
    // partitioned
    jdbcClient
            .sql("copy (from 'tls_test.json') to 'tls_output.parquet' ")
            .update();


  }

}
