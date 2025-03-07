package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public class WebRepository {

  private final String dataLocation;
  private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);
  private final JdbcClient jdbcClient;
  
  private final ObjectMapper objectMapper;

  public WebRepository(@Value("${mercator.data.location:mercator/data/}") String dataLocation) {

    if (dataLocation == null || dataLocation.isEmpty()) {
      throw new IllegalArgumentException("dataLocation must not be null or empty");
    }
    if (dataLocation.endsWith("/")) {
      this.dataLocation = dataLocation;
    } else {
      this.dataLocation = dataLocation + "/";
    }
    logger.info("dataLocation = [{}]", dataLocation);
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule()); // Support for Instant
    this.objectMapper.registerModule(new Jdk8Module()); // Support for Instantthis.jdbcClient = JdbcClient.create(DuckDataSource.memory());
    this.jdbcClient = JdbcClient.create(DuckDataSource.memory());
    }

  public List<String> searchVisitIds(String domainName) {
    // TODO
    logger.info("Searching visitIds for domainName={}", domainName);
    return List.of();
  }

  public Optional<WebCrawlResult> findLatestResult(String domainName) {
    // TODO
    logger.info("Finding latest crawl result for domainName={}", domainName);
    return Optional.empty();
  }

  public Optional<WebCrawlResult> findByVisitId(String visitId) {
   // TODO
    logger.info("Finding latest crawl result for visitId={}", visitId);
    return Optional.empty();
  }

  public void saveToParquet(Path jsonFile, String parquetFileName) {
    // TODO: partitioned
    // TODO
    // copy json to parquet
    // rename fields
    // partitioned
    // noinspection SqlSourceToSinkFlow
    jdbcClient
            .sql("")
            .update();
    logger.info("Parquet file name = [{}]", parquetFileName + "with pathname: " + jsonFile.toString());


  }

}
