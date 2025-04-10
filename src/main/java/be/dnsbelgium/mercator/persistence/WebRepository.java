package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

@Component
public class WebRepository extends BaseRepository<WebCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);

    private final String webCrawlDestination;
    private final String pageVisitDestination;
    private final String featuresDestination;

    @SneakyThrows
    public WebRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, WebCrawlResult.class);
        String subPath = "web";
        webCrawlDestination = createDestination(baseLocation, subPath, "crawl_result");
        pageVisitDestination = createDestination(baseLocation, subPath, "page_visit");
        featuresDestination = createDestination(baseLocation, subPath, "html_features");
    }

    public void setVariables(JdbcClient jdbcClient) {
        jdbcClient.sql("set variable webCrawlDestination = ?")
                .param(webCrawlDestination)
                .update();
        jdbcClient.sql("set variable pageVisitDestination = ?")
                .param(pageVisitDestination)
                .update();
        jdbcClient.sql("set variable featuresDestination = ?")
                .param(featuresDestination)
                .update();
    }

    @Override
    public String getAllItemsQuery() {
        String allItemsQuery = readFromClasspath("sql/web/get_all_items.sql");
        logger.debug("allItemsQuery: {}", allItemsQuery);
        return allItemsQuery;
    }

    @Override
    public String timestampField() {
        return "crawl_started";
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
        try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
            String cteDefinitions = readFromClasspath("sql/web/cte_definitions.sql");
            logger.debug("cteDefinitions: {}", cteDefinitions);
            copyToParquet(jsonResultsLocation, dataSource, cteDefinitions, "web_crawl_result", webCrawlDestination);
            copyToParquet(jsonResultsLocation, dataSource, cteDefinitions, "page_visits", pageVisitDestination);
            copyToParquet(jsonResultsLocation, dataSource, cteDefinitions, "html_features_casted", featuresDestination);
        }
    }
}