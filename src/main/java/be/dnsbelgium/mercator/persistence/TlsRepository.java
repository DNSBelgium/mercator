package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class TlsRepository extends BaseRepository<TlsCrawlResult> {

  private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);

  public TlsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String dataLocation) {
    super(objectMapper, dataLocation, JdbcClient.create(DuckDataSource.memory()), TlsCrawlResult.class);
  }

  @Override
  public String domainNameField() {
    return "visit_request.domain_name";
  }
}
