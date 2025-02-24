package be.dnsbelgium.mercator.wappalyzer.crawler.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.simple.JdbcClient;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.dnsbelgium.mercator.common.VisitIdGenerator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

public class TechnologyAnalyzerWebCrawlRepositoryTest {

    static DuckDataSource dataSource;
    static TechnologyAnalyzerWebCrawlRepository technologyAnalyzerWebCrawlRepository;
    static JdbcClient jdbcClient;
    static MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static final Logger logger = LoggerFactory.getLogger(TechnologyAnalyzerWebCrawlRepositoryTest.class);

    @BeforeAll
    public static void init() {
        dataSource = new DuckDataSource("jdbc:duckdb:");
        technologyAnalyzerWebCrawlRepository = new TechnologyAnalyzerWebCrawlRepository(dataSource, meterRegistry);
        jdbcClient = JdbcClient.create(dataSource);
        technologyAnalyzerWebCrawlRepository.createTables();
        logger.debug("init done for technologyzanalyzerwebcrawlrepotest");

    }

    @AfterEach
    public void cleanUp() {
        jdbcClient.sql("delete from technology_analyzer_web_crawl_result");
    }

    @Test
    public void givenTechnologyAnalyzerWebCrawlRepository_whenCreateTables_thenTablesCreated() {
        technologyAnalyzerWebCrawlRepository.createTables();
        logger.info("createTables done");
        assertThat(technologyAnalyzerWebCrawlRepository).isNotNull();
    }

    @Test
    public void givenTechnologyAnalyzerWebCrawlRepository_whenInsert_thenDataInserted() {
        String visitId = VisitIdGenerator.generate();
        String domainName = "www.wikipedia.org";
        String[] detectedTechnologies = { "Java", "Spring" };

        TechnologyAnalyzerWebCrawlResult crawlResult = TechnologyAnalyzerWebCrawlResult.builder()
                .visitId(visitId)
                .domainName(domainName)
                .detectedTechnologies(Set.of(detectedTechnologies))
                .build();

        technologyAnalyzerWebCrawlRepository.saveTechnologyAnalyzerWebCrawlResult(crawlResult);
        logger.debug("insert done for technologyzanalyzerwebcrawlrepotest");

        List<Map<String, Object>> result = jdbcClient.sql("select * from technology_analyzer_web_crawl_result;").query()
                .listOfRows();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).get("visit_id")).isEqualTo(visitId);
        assertThat(result.get(0).get("domain_name")).isEqualTo(domainName);

        // convert duckdb array into a string and then convert back to an array
        String techonologyStringObject = result.get(0).get("detected_technologies").toString();
        String[] resultArray = techonologyStringObject.replaceAll("[\\[\\]]", "").split(", ");

        assertThat(resultArray).containsExactlyInAnyOrder(detectedTechnologies);
    }

    @Test
    public void givenTechnologyAnalyzerWebCrawlRepository_WhenFindTechnologyAnalyzerWebCrawlResultsByVisitId_ThenResultsReturned() {
        String visitId = VisitIdGenerator.generate();
        String domainName = "www.wikipedia.org";
        String[] detectedTechnologies = { "Java", "Spring" };
        TechnologyAnalyzerWebCrawlResult crawlResult = TechnologyAnalyzerWebCrawlResult.builder()
                .visitId(visitId)
                .domainName(domainName)
                .detectedTechnologies(Set.of(detectedTechnologies))
                .build();
        technologyAnalyzerWebCrawlRepository.saveTechnologyAnalyzerWebCrawlResult(crawlResult);
        List<TechnologyAnalyzerWebCrawlResult> results = technologyAnalyzerWebCrawlRepository
                .findTechnologyAnalyzerWebCrawlResults(visitId);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getVisitId()).isEqualTo(visitId);
        assertThat(results.get(0).getDomainName()).isEqualTo(domainName);
        assertThat(results.get(0).getDetectedTechnologies()).isEqualTo(Set.of(detectedTechnologies));
    }

}
