package be.dnsbelgium.mercator.vat.crawler.persistence;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static be.dnsbelgium.mercator.test.TestUtils.now;
import static org.assertj.core.api.Assertions.assertThat;

class WebRepositoryTest {

    static DuckDataSource dataSource;
    static WebRepository webRepository;
    static JdbcClient jdbcClient;
    static MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static final Logger logger = LoggerFactory.getLogger(WebRepositoryTest.class);

    @BeforeAll
    public static void init() {
        dataSource = new DuckDataSource("jdbc:duckdb:");
        webRepository = new WebRepository(dataSource, meterRegistry);
        jdbcClient = JdbcClient.create(dataSource);
        webRepository.createTables();
        logger.info("init done");
    }


    @Test
    public void savePageVisit() {
        var vat_values = List.of("double quote \" here", "abc", "", "[0]{1}(2)'x'");
        PageVisit pageVisit = PageVisit.builder()
                .visitId(VisitIdGenerator.generate())
                .path("/example?id=455")
                .url("https://www.google.com")
                .bodyText("This is a test")
                .crawlFinished(now())
                .crawlStarted(now().minusMillis(456))
                .domainName("google.com")
                .vatValues(vat_values)
                .statusCode(0)
                .linkText("contact us")
                .build();
        webRepository.save(pageVisit);
        List<PageVisit> pageVisits = webRepository.findPageVisits(pageVisit.getVisitId());
        pageVisits.forEach(System.out::println);
        assertThat(pageVisits).hasSize(1);
        PageVisit found = pageVisits.getFirst();
        assertThat(found.getVisitId()).isEqualTo(pageVisit.getVisitId());
        assertThat(found.getVatValues()).isEqualTo(pageVisit.getVatValues());
        assertThat(found.getHtml()).isEqualTo(pageVisit.getHtml());
        assertThat(found.getBodyText()).isEqualTo(pageVisit.getBodyText());
        assertThat(found.getUrl()).isEqualTo(pageVisit.getUrl());
        assertThat(found.getDomainName()).isEqualTo(pageVisit.getDomainName());
        assertThat(found.getLinkText()).isEqualTo(pageVisit.getLinkText());
        assertThat(found.getStatusCode()).isEqualTo(pageVisit.getStatusCode());
        assertThat(found.getPath()).isEqualTo(pageVisit.getPath());
        assertThat(found.getCrawlStarted()).isEqualTo(pageVisit.getCrawlStarted());
        assertThat(found.getCrawlFinished()).isEqualTo(pageVisit.getCrawlFinished());
        assertThat(found).isEqualTo(pageVisit);
    }


    @Test
    public void webVisit() {
        String visitId = VisitIdGenerator.generate();
        WebCrawlResult crawlResult = WebCrawlResult
                .builder()
                .visitId(visitId)
                .crawlStarted(now())
                .crawlFinished(now().plusMillis(126))
                .domainName("google.be")
                .visitedUrls(List.of("https://www.google.be", "https://google.com?countr=be"))
                .build();
        webRepository.saveWebVisit(crawlResult);
        List<Map<String, Object>> rows = jdbcClient.sql("select * from web_visit").query().listOfRows();
        System.out.println("rows = " + rows);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("visit_id").toString()).isEqualTo(crawlResult.getVisitId());
        assertThat(rows.getFirst().get("domain_name")).isEqualTo(crawlResult.getDomainName());

        List<WebCrawlResult> list = webRepository.findWebCrawlResult(visitId);
        assertThat(list).hasSize(1);
        WebCrawlResult found = list.getFirst();
        assertThat(found.getVisitId()).isEqualTo(visitId);
        assertThat(found.getVatValues()).isEqualTo(crawlResult.getVatValues());
        assertThat(found.getDomainName()).isEqualTo(crawlResult.getDomainName());
        assertThat(found.getCrawlStarted()).isEqualTo(crawlResult.getCrawlStarted());
        assertThat(found.getCrawlFinished()).isEqualTo(crawlResult.getCrawlFinished());
        assertThat(found.getVisitedUrls()).isEqualTo(crawlResult.getVisitedUrls());
    }

    @Test
    public void htmlFeatures() {
        HtmlFeatures htmlFeatures = new HtmlFeatures();
        htmlFeatures.visitId = VisitIdGenerator.generate();
        htmlFeatures.domainName = "google.com";
        htmlFeatures.crawlTimestamp = Instant.now();
        htmlFeatures.body_text = "hello world";
        htmlFeatures.external_hosts = List.of("google.com", "facebook.com");
        htmlFeatures.linkedin_links = List.of("linkedin.com/abc", "https://linkedin.com/xxx");
        webRepository.save(htmlFeatures);
    }

    @Test
    public void findFeatures() {
        HtmlFeatures htmlFeatures = new HtmlFeatures();
        htmlFeatures.visitId = VisitIdGenerator.generate();
        htmlFeatures.domainName = "google.com";
        htmlFeatures.crawlTimestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        htmlFeatures.body_text = "hello world";
        htmlFeatures.external_hosts = List.of("google.com", "facebook.com");
        htmlFeatures.linkedin_links = List.of("linkedin.com/abc", "https://linkedin.com/xxx");
        webRepository.save(htmlFeatures);
        List<HtmlFeatures> found = webRepository.findHtmlFeatures(htmlFeatures.visitId);
        System.out.println("found = " + found);
        System.out.println("found.external_hosts = " + found.getFirst().external_hosts.size());
        System.out.println("external_hosts[0] = " + found.getFirst().external_hosts.get(0));
        System.out.println("external_hosts[1] = " + found.getFirst().external_hosts.get(1));
        assertThat(found.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(htmlFeatures);
    }

}