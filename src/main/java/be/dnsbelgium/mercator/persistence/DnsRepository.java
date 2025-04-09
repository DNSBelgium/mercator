package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    @Override
    public String getAllItemsQuery() {
        String allItemsQuery = readFromClasspath("sql/dns/get_all_items.sql");
        String query = StringSubstitutor.replace(allItemsQuery, Map.of(
                "geoIpResponseDestination", this.geoIpDestination,
                "responseDestination", this.responseDestination,
                "requestDestination", this.requestDestination
        ));
        logger.info("query: {}", query);
        return query;
    }

    @Override
    public void storeResults(String jsonLocation) {
        try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
            String cteDefinitions = readFromClasspath("sql/dns/cte_definitions.sql");
            logger.debug("cteDefinitions: {}", cteDefinitions);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "requests", requestDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "responses", responseDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "geo_ips_casted", geoIpDestination);
        }
    }

}
