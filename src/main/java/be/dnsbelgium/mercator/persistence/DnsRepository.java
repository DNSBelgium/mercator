package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DnsRepository extends BaseRepository<DnsCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(DnsRepository.class);

    private final String baseLocation;

    @SneakyThrows
    public DnsRepository(JdbcClientFactory jdbcClientFactory, ObjectMapper objectMapper,
                         @Value("${mercator.data.location:mercator/data/}/dns") String baseLocation,
                         @Value("${test.access.to.data.location:false}") boolean testAccessToDataLocation) {
        super(jdbcClientFactory, objectMapper, baseLocation, DnsCrawlResult.class);
        this.baseLocation = baseLocation;
        logger.info("testAccessToDataLocation = {}", testAccessToDataLocation);
        if (testAccessToDataLocation) {
          testAccessToBaseLocation();
        }
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
       String cteDefinitions = readFromClasspath("sql/dns/cte_definitions.sql");
       logger.debug("cteDefinitions: {}", cteDefinitions);
       copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);
    }

}
