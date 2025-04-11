package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

@Component
public class DnsRepository extends BaseRepository<DnsCrawlResult> {

    private static final Logger logger = LoggerFactory.getLogger(DnsRepository.class);

    private final String requestDestination;
    private final String responseDestination;
    private final String geoIpDestination;

    @SneakyThrows
    public DnsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, DnsCrawlResult.class);
        String subPath = "dns";
        requestDestination = createDestination(baseLocation, subPath, "requests");
        responseDestination = createDestination(baseLocation, subPath, "responses");
        geoIpDestination = createDestination(baseLocation, subPath, "geoips");
    }

    public void setVariables(JdbcClient jdbcClient) {
        jdbcClient.sql("set variable geoIpDestination = ?")
                .param(geoIpDestination)
                .update();
        jdbcClient.sql("set variable responseDestination = ?")
                .param(responseDestination)
                .update();
        jdbcClient.sql("set variable requestDestination = ?")
                .param(requestDestination)
                .update();
    }

    @Override
    public String getAllItemsQuery() {
        String allItemsQuery = readFromClasspath("sql/dns/get_all_items.sql");
        logger.info("allItemsQuery: {}", allItemsQuery);
        return allItemsQuery;
    }

    @Override
    public void storeResults(String jsonLocation) {
        try (SingleConnectionDataSource dataSource = singleThreadedDataSource()) {
            String cteDefinitions = readFromClasspath("sql/dns/cte_definitions.sql");
            logger.debug("cteDefinitions: {}", cteDefinitions);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "requests", requestDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "responses", responseDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "geo_ips_casted", geoIpDestination);
        }
    }

}
