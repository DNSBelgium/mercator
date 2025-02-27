package be.dnsbelgium.mercator.mvc.repository;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;


import be.dnsbelgium.mercator.persistence.DuckDataSource;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.core.simple.JdbcClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class SearchRepository {
    private static final Logger logger = LoggerFactory.getLogger(SearchRepository.class);

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public SearchRepository() {
        this.jdbcClient = JdbcClient.create(DuckDataSource.memory());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Support for Instant
    }

    public List<WebCrawlResult> searchVisitIds(String domainName) {
        List<String> jsonResults = jdbcClient
                .sql("select to_json(p) from 'web.parquet' p where domainName = ? limit 10")
                .param(domainName)
                .query(String.class)
                .list();

        List<WebCrawlResult> webCrawlResults = jsonResults.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, WebCrawlResult.class);
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse JSON: {}", json, e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();

        logger.info("our results: " + webCrawlResults.toString());
        return webCrawlResults;
    }

    public Optional<String> searchVisitIdWithOption(String visitId, String option)  {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        // get one row from the parquet file
        Optional<String> json = jdbcClient
                .sql("select to_json(p) from '" + option + ".parquet' p where visitId = ? limit 10")
                .param(visitId)
                .query(String.class)
                .optional();

        return json;

    }
}

