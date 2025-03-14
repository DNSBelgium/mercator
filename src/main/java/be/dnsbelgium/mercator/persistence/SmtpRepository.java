package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;

import com.fasterxml.jackson.databind.ObjectMapper;


import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class SmtpRepository {
    private static final Logger logger = LoggerFactory.getLogger(SmtpRepository.class);
    private final String dataLocation;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public SmtpRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String dataLocation) {
        this.dataLocation = dataLocation;
        this.jdbcClient = JdbcClient.create(DuckDataSource.memory());
        this.objectMapper = objectMapper;
    }

    public List<String> searchVisitIds(String domainName) {
        // TODO
        logger.info("Searching visitIds for domainName={}", domainName);
        return List.of();
    }

    public Optional<SmtpConversation> findLatestResult(String domainName) {
        // TODO
        logger.info("Finding latest crawl result for domainName={}", domainName);
        return Optional.empty();
    }

    public Optional<SmtpConversation> findByVisitId(String visitId) {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        Path parquetFilePath = Path.of("test");

        // TODO: use location given to teh repository to search instead of hardcoded
        String query = String.format("select to_json(p) from '%s' p where visit_id = ?", parquetFilePath);
        return jdbcClient
                .sql(query)
                .param(visitId)
                .query(SmtpConversation.class)
                .optional();
    }

    public void saveToParquet(Path jsonFile, String parquetFileName) {
        // TODO: partitioned
        String path = dataLocation + File.separator + parquetFileName;
        // copy json to parquet
        // rename fields
        String query = String.format("COPY (FROM '%s') TO '%s' (FORMAT parquet)", jsonFile, path);
        // partitioned
        //noinspection SqlSourceToSinkFlow
        jdbcClient
                .sql(query)
                .update();
        logger.info("Saved to parquet file at {}", path);
    }



}


