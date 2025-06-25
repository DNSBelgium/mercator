package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;


@Repository
public class SmtpRepository extends BaseRepository<SmtpVisit> {

    private static final Logger logger = LoggerFactory.getLogger(SmtpRepository.class);

    private final String baseLocation;

    @SneakyThrows
    public SmtpRepository(JdbcClient jdbcClient, ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}/smtp") String baseLocation) {
        super(jdbcClient, objectMapper, baseLocation, SmtpVisit.class);
        this.baseLocation = baseLocation;
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
        String cteDefinitions = readFromClasspath("sql/smtp/cte_definitions.sql");
        logger.debug("cteDefinitions: {}", cteDefinitions);
        copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);
    }
}