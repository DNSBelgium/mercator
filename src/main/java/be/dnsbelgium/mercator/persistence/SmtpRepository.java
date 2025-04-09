package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import com.fasterxml.jackson.databind.ObjectMapper;


import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Repository;


@Repository
public class SmtpRepository extends BaseRepository<SmtpVisit> {
    private static final Logger logger = LoggerFactory.getLogger(SmtpRepository.class);

    private final String smtpVisitDestination;
    private final String smtpHostDestination;

    @SneakyThrows
    public SmtpRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, SmtpVisit.class);
        String subPath = "smtp";
        smtpVisitDestination = createDestination(baseLocation, subPath, "visits");
        smtpHostDestination = createDestination(baseLocation, subPath, "hosts");
    }

    @Override
    public String getAllItemsQuery() {
        String allItemsQuery = readFromClasspath("sql/smtp/get_all_items.sql");
        String query = StringSubstitutor.replace(allItemsQuery, Map.of(
                "smtpVisitDestination", this.smtpVisitDestination,
                "smtpHostDestination", this.smtpHostDestination
        ));
        logger.info("query: {}", query);
        return query;
    }

    @Override
    public String timestampField(){
        return "timestamp";
    }

    @Override
    public void storeResults(String jsonLocation) {
        try (var dataSource = new SingleConnectionDataSource("jdbc:duckdb:", false)) {
            String cteDefinitions = readFromClasspath("sql/smtp/cte_definitions.sql");
            logger.debug("cteDefinitions: {}", cteDefinitions);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "smtp_visit_results", smtpVisitDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "smtp_hosts", smtpHostDestination);
        }
    }

}