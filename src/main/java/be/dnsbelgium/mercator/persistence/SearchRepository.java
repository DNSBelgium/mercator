package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;


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
    
    public List<WebCrawlResult> searchVisitIdsWeb(String domainName) {
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

    public List<SmtpConversation> searchVisitIdsSmtp(String domainName) {
        List<String> jsonResults = jdbcClient
                .sql("select to_json(p) from 'smtp.parquet' p where domainName = ? limit 10")
                .param(domainName)
                .query(String.class)
                .list();

        List<SmtpConversation> smtpConversations = jsonResults.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, SmtpConversation.class);
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse JSON: {}", json, e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();

        logger.info("our results: " + smtpConversations.toString());
        return smtpConversations;
    }

    public List<TlsCrawlResult> searchVisitIdsTls(String domainName) {
        List<String> jsonResults = jdbcClient
                .sql("select to_json(p) from 'tls.parquet' p where visitRequest.domainName = ? limit 10")
                .param(domainName)
                .query(String.class)
                .list();

        List<TlsCrawlResult> tlsCrawlResults = jsonResults.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, TlsCrawlResult.class);
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse JSON: {}", json, e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();

        logger.info("our results: " + tlsCrawlResults.toString());
        return tlsCrawlResults;
    }

    public Optional<String> searchVisitIdWeb(String visitId)  {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        Optional<String> json = jdbcClient
                .sql("select to_json(p) from 'web.parquet' p where CAST(p.visitId as varchar) = ? limit 10"
                )
                .param(visitId)
                .query(String.class)
                .optional();
        return json;

    }

    public Optional<String> searchVisitIdSmtp(String visitId)  {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        Optional<String> json = jdbcClient
                .sql("select to_json(p) from 'smtp.parquet' p where CAST(p.visitId as varchar) = ? limit 10"
                )
                .param(visitId)
                .query(String.class)
                .optional();
        return json;

    }


    public Optional<String> searchVisitIdTls(String visitId)  {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        Optional<String> json = jdbcClient // condition because tls contains the visitId in visitRequest
                .sql("SELECT to_json(p) FROM 'tls.parquet' p WHERE CAST(p.visitRequest.visitId AS VARCHAR) = ? LIMIT 10")
                .param(visitId)
                .query(String.class)
                .optional();

        return json;


    }

    public Optional<String> searchlatestTlsResult()  {

        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        String visitId = jdbcClient.sql("SELECT p.visitRequest.visitId FROM 'tls.parquet' p ORDER BY fullScanEntity.crawlTimestamp DESC LIMIT 1")
            .query(String.class)
                .single();
        System.out.println(visitId);
        Optional<String> json = jdbcClient // condition because tls contains the visitId in visitRequest
                .sql("SELECT to_json(p) FROM 'tls.parquet' p WHERE CAST(p.visitRequest.visitId AS VARCHAR) = ?")
                .param(visitId)
                .query(String.class)
                .optional();

        return json;


    }

    public Optional<String> searchlatestSmtpResult()  {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        String visitId = jdbcClient.sql("SELECT p.visitId FROM 'smtp.parquet' p ORDER BY timestamp DESC LIMIT 1")
                .query(String.class)
                .single();
        System.out.println(visitId);
        Optional<String> json = jdbcClient // condition because tls contains the visitId in visitRequest
                .sql("SELECT to_json(p) FROM 'smtp.parquet' p WHERE CAST(p.visitId AS VARCHAR) = ?")
                .param(visitId)
                .query(String.class)
                .optional();
        return json;

    }

    public Optional<String> searchlatestWebResult()  {
        JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
        String visitId = jdbcClient.sql("SELECT p.visitId FROM 'web.parquet' p ORDER BY crawlFinished DESC LIMIT 1")
                .query(String.class)
                .single();
        System.out.println(visitId);
        Optional<String> json = jdbcClient // condition because tls contains the visitId in visitRequest
                .sql("SELECT to_json(p) FROM 'web.parquet' p WHERE CAST(p.visitId AS VARCHAR) = ?")
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
        Optional<String> json = searchVisitIdTls(visitId);
        if (json.isPresent()) {
            TlsCrawlResult tlsCrawlResult = objectMapper.readValue(json.get(), TlsCrawlResult.class);;
            return tlsCrawlResult;
        }
        return null;
    }

    public SmtpConversation findSmtpConversationResult(String visitId) throws JsonProcessingException {
        Optional<String> json = searchVisitIdSmtp(visitId);
        if (json.isPresent()) {
            SmtpConversation smtpConversation = objectMapper.readValue(json.get(), SmtpConversation.class);
            return smtpConversation;
        }
        return null;
    }



}


