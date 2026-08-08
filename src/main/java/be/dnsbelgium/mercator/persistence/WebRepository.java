package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WebRepository extends BaseRepository<WebCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);

    private final String baseLocation;
    private final String responseBodyLocation;

    @SneakyThrows
    public WebRepository(JdbcClientFactory jdbcClientFactory, ObjectMapper objectMapper,
                         @Value("${mercator.data.location:mercator/data/}/web") String baseLocation,
                         @Value("${mercator.data.location:mercator/data/}/web_response_body") String responseBodyLocation)
    {
        super(jdbcClientFactory, objectMapper, baseLocation, WebCrawlResult.class);
        this.baseLocation = baseLocation;
        this.responseBodyLocation = createDestination(responseBodyLocation);
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
        String cteDefinitions = readFromClasspath("sql/web/cte_definitions.sql");
        logger.debug("cteDefinitions: {}", cteDefinitions);
        copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);

        String cteDefinitionsResponseBody = readFromClasspath("sql/web/cte_definitions_response_body.sql");
        logger.debug("cteDefinitionsResponseBody: {}", cteDefinitionsResponseBody);
        copyToParquet(jsonResultsLocation, cteDefinitionsResponseBody, "response_body_with_year_month", responseBodyLocation);
    }

    public Optional<String> getResponseBody(int year, int month, String visitId, String finalUrl) {
      JdbcClient jdbcClient = jdbcClientFactory.jdbcClient();
      String select = """
              select response_body
              from '%s/**/*.parquet'
              where visit_id = :visitId
                and final_url = :finalUrl
                and year = :year
                and month = :month
              order by crawl_started desc
              limit 1
      """.formatted(responseBodyLocation);

      logger.debug("select = \n{}", select);

      Optional<String> body = jdbcClient
              .sql(select)
              .param("visitId", visitId)
              .param("finalUrl", finalUrl)
              .param("year", year)
              .param("month", month)
              .query(String.class)
              .optional();
      if (body.isPresent()) {
        logger.info("VisitId={}, finalUrl={} year={}, month={} => found response_body of {} chars", visitId, finalUrl, year, month, body.get().length());
      } else {
        logger.info("VisitId={}, finalUrl={}, year={}, month={}=> No response_body found", visitId, finalUrl, year, month);
      }
      return body;
    }

}