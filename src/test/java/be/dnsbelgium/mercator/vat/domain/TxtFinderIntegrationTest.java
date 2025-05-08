package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.WebCrawler;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
// These test make an internet connections, set the env var WEB_OUTBOUND_TEST_ENABLED to "True" to enable the tests
// Running one individual test method in IntelliJ also seems to work (it seems to ignore the @EnabledIfEnvironmentVariable)
public class TxtFinderIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TxtFinderIntegrationTest.class);

    @Autowired
    private WebCrawler webCrawler;

    private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());

    @Test
    @EnabledIfEnvironmentVariable(named = "WEB_OUTBOUND_TEST_ENABLED", matches = "true")
    public void testFindSecurityTxt() {
        VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), "dnsbelgium.be");
        PageVisit foundPage = webCrawler.findSecurityTxt(visitRequest);
        assertThat(foundPage.getResponseBody()).isNotEmpty();
        assertThat(foundPage.getHeaders().toString()).isNotEmpty();
        logger.info("body text: {}", foundPage.getResponseBody());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEB_OUTBOUND_TEST_ENABLED", matches = "true")
    public void testFindSecurityTxtOnPopularDomainNames() {
        List<String> domainNames = getTop(200);
        List<PageVisit> pageVisits =  new ArrayList<>();
        for (String domainName : domainNames) {
            VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), domainName);
            try {
                PageVisit found = webCrawler.findSecurityTxt(visitRequest);
                pageVisits.add(found);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        save(pageVisits, "security_txt_top100.json");
        int count200 = jdbcClient
                .sql("select count(1) from './target/test-outputs/security_txt_top100.json' where status_code = 200")
                .query(Integer.class)
                .single();
        logger.info("count200 = {}", count200);

    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEB_OUTBOUND_TEST_ENABLED", matches = "true")
    public void testFindRobotsTxt() {
        VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), "dnsbelgium.be");
        PageVisit found = webCrawler.findRobotsTxt(visitRequest);
        assertThat(found.getResponseBody()).isNotEmpty();
        assertThat(found.getHeaders().toString()).isNotEmpty();
        logger.info("found.getBodyText = {}", found.getResponseBody());
        logger.info("found.getContentLength = {}", found.getContentLength());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WEB_OUTBOUND_TEST_ENABLED", matches = "true")
    public void testFindRobotsTxtOnPopularDomainNames() {
        List<String> domainNames = getTop(100);
        List<PageVisit> pageVisits =  new ArrayList<>();
        for (String domainName : domainNames) {
            VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), domainName);
            try {
                PageVisit found = webCrawler.findRobotsTxt(visitRequest);
                pageVisits.add(found);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        save(pageVisits, "robots_txt_top100.json");
        int count200 = jdbcClient
                .sql("select count(1) from './target/test-outputs/robots_txt_top100.json' where status_code = 200")
                .query(Integer.class)
                .single();
        logger.info("count200 = {}", count200);
    }

    @SneakyThrows
    private void save(List<PageVisit> pageVisits, String filename) {
        File parent = new File("./target/test-outputs/");
        Files.createDirectories(parent.toPath());
        File file = new File(parent, filename);
        TestUtils.jsonWriter()
                    .writeValue(file, pageVisits);
        logger.info("JSON data written to file {}", file);
    }

    private List<String> getTop(int limit) {
        List<String> domainNames = jdbcClient
                .sql("select domain_name from 'tranco_be.parquet' order by tranco_rank limit ?")
                .param(limit)
                .query(String.class)
                .list();
        logger.info("Found top {} domainNames", domainNames.size());
        return domainNames;
    }


}
