package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import com.fasterxml.jackson.databind.ObjectMapper;


import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.simple.JdbcClient;
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

    public void setVariables(JdbcClient jdbcClient) {
        jdbcClient.sql("set variable smtpVisitDestination = ?")
                .param(smtpVisitDestination)
                .update();
        jdbcClient.sql("set variable smtpHostDestination = ?")
                .param(smtpHostDestination)
                .update();
    }

    @Override
    public String getAllItemsQuery() {
        String allItemsQuery = readFromClasspath("sql/smtp/get_all_items.sql");
        logger.info("allItemsQuery: {}", allItemsQuery);
        return allItemsQuery;
    }

    @Override
    public String timestampField(){
        return "timestamp";
    }

    @Override
    public void storeResults(String jsonLocation) {
        try (SingleConnectionDataSource dataSource = singleThreadedDataSource()) {
            String cteDefinitions = readFromClasspath("sql/smtp/cte_definitions.sql");
            logger.debug("cteDefinitions: {}", cteDefinitions);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "smtp_visit_results", smtpVisitDestination);
            copyToParquet(jsonLocation, dataSource, cteDefinitions, "smtp_hosts", smtpHostDestination);
        }
    }

}