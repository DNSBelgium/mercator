package be.dnsbelgium.mercator.mvc.repository;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;


import be.dnsbelgium.mercator.persistence.DuckDataSource;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
        this.objectMapper.registerModule(new Jdk8Module()); // Support for Instant
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
        if (option.equals("tls")) {
            Optional<String> json = jdbcClient // condition because tls contains the visitId in visitRequest
                    .sql("SELECT to_json(p) FROM '" + option + ".parquet' p WHERE CAST(p.visitRequest.visitId AS VARCHAR) = ? LIMIT 10")
                    .param(visitId)
                    .query(String.class)
                    .optional();

            return json;
        }
        // get one row from the parquet file
        Optional<String> json = jdbcClient
                .sql("select to_json(p) from " + option + ".parquet p where CAST(p.visitId as varchar) = ? limit 10"
                )
                .param(visitId)
                .query(String.class)
                .optional();

        return json;

    }

    public WebCrawlResult findWebCrawlResult(String visitId) {
//        Optional<String> json = searchVisitIdWithOption(visitId, option);
//        if (json.isPresent()) {
//            try {
//                if ("web".equals(option)) {
//                    WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);;
//                    model.addAttribute("webCrawlResults", List.of(webCrawlResult));
//                    logger.info("webCrawlResult = {}", webCrawlResult);
//
//                }
        // TODO
        return null;
    }

    public TlsCrawlResult findTlsCrawlResult(String visitId) throws JsonProcessingException {
        Optional<String> json = searchVisitIdWithOption(visitId, "tls");
        if (json.isPresent()) {
            TlsCrawlResult tlsCrawlResult = objectMapper.readValue(json.get(), TlsCrawlResult.class);;
            return tlsCrawlResult;
        }
        return null;
    }


}


