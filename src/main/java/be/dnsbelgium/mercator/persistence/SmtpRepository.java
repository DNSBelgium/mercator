package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import be.dnsbelgium.mercator.pipeline.config.PipelineJacksonConfig;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;


@Repository
public class SmtpRepository extends BaseRepository<SmtpVisit> {

    private static final Logger logger = LoggerFactory.getLogger(SmtpRepository.class);

    private final String baseLocation;

    @SneakyThrows
    public SmtpRepository(JdbcClientFactory jdbcClientFactory,
                          @Qualifier(PipelineJacksonConfig.PIPELINE_OBJECT_MAPPER) ObjectMapper objectMapper,
                          @Value("${mercator.data.location:mercator/data/}/smtp") String baseLocation) {
        super(jdbcClientFactory, objectMapper, baseLocation, SmtpVisit.class);
        this.baseLocation = baseLocation;
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
        String cteDefinitions = readFromClasspath("sql/smtp/cte_definitions.sql");
        logger.debug("cteDefinitions: {}", cteDefinitions);
        copyToParquet(jsonResultsLocation, cteDefinitions, "added_year_month", baseLocation);
    }
}