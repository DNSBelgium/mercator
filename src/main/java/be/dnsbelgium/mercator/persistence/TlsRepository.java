package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class TlsRepository extends BaseRepository<TlsCrawlResult> {

  private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);

  private final String baseLocation;

  @SneakyThrows
  public TlsRepository(JdbcClientFactory jdbcClientFactory, ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}/tls") String baseLocation) {
    super(jdbcClientFactory, objectMapper, baseLocation, TlsCrawlResult.class);
    this.baseLocation = baseLocation;
  }

  @Override
  public void storeResults(String jsonResultsLocation) {
    String cteDefinitions = readFromClasspath("sql/tls/cte_definitions.sql");
    logger.debug("cteDefinitions: {}", cteDefinitions);
    copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);
  }

}
