package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

@Component
public class TlsRepository extends BaseRepository<TlsCrawlResult> {

  private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);

  private String baseLocation;

  @SneakyThrows
  public TlsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}/tls") String baseLocation) {
    super(objectMapper, baseLocation, TlsCrawlResult.class);
    this.baseLocation = baseLocation;
  }

  @Override
  public String timestampField() {
    return "crawl_timestamp";
  }

  @Override
  public void storeResults(String jsonResultsLocation) {
    String cteDefinitions = readFromClasspath("sql/tls/cte_definitions.sql");
    logger.debug("cteDefinitions: {}", cteDefinitions);
    copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);
  }

}
