package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.WebCrawler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
public class TxtFinderIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TxtFinderIntegrationTest.class);

    @Autowired
    private WebCrawler webCrawler;


    private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());

    @Test
    @Disabled
    // this test makes an internet connection
    public void testFindSecurityTxt() {
        VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), "dnsbelgium.be");
        PageVisit updatedVisit = webCrawler.findSecurityTxt(visitRequest);
        assertThat(updatedVisit.getBodyText()).isNotEmpty();
        assertThat(updatedVisit.getHeaders().toString()).isNotEmpty();
        assertThat(updatedVisit.getContentLength()).isGreaterThan(0);
        logger.info(updatedVisit.getBodyText());
        logger.info(updatedVisit.getHtml());
        assertThat(updatedVisit.getBodyText()).isNotEqualTo(updatedVisit.getHtml());
    }

    @Test
    @Disabled
    // this test makes an internet connection
    public void testFindSecurityTxtOnPopularDomainNames() {
        List<String> domainNames = jdbcClient
                .sql("select domain_name from 'tranco_be.parquet' order by tranco_rank limit 100")
                .query(String.class)
                .list();
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
        try {
            TestUtils.jsonWriter()
                    .writeValue(new File("security_txt_top100.json"), pageVisits);
            logger.info("JSON data written to file ");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    @Test
    @Disabled
    public void testFindRobotsTxt() {
        VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), "dnsbelgium.be");
        PageVisit updatedVisit = webCrawler.findRobotsTxt(visitRequest);
        assertThat(updatedVisit.getBodyText()).isNotEmpty();
        assertThat(updatedVisit.getHeaders().toString()).isNotEmpty();
        assertThat(updatedVisit.getContentLength()).isGreaterThan(0);
    }

    @Test
    @Disabled
    public void testFindRobotsTxtOnPopularDomainNames() {
        List<String> domainNames = jdbcClient
                .sql("select domain_name from 'tranco_be.parquet' order by tranco_rank limit 100")
                .query(String.class)
                .list();
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
        try {
            TestUtils.jsonWriter()
                    .writeValue(new File("robots_txt_top100.json"), pageVisits);
                                                                              logger.info("JSON data written to file ");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }






}
