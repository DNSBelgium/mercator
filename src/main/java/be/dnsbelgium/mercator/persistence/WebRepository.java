package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class WebRepository extends BaseRepository<WebCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);

    private final String baseLocation;

    @SneakyThrows
    public WebRepository(JdbcClient jdbcClient, ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}/web") String baseLocation) {
        super(jdbcClient, objectMapper, baseLocation, WebCrawlResult.class);
        this.baseLocation = baseLocation;
    }

    @Override
    public String timestampField() {
        return "crawl_started";
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
        String cteDefinitions = readFromClasspath("sql/web/cte_definitions.sql");
        logger.debug("cteDefinitions: {}", cteDefinitions);
        copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);
    }
}