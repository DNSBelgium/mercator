package be.dnsbelgium.mercator.wappalyzer.crawler.persistence;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import be.dnsbelgium.mercator.persistence.ArrayConvertor;

import javax.sql.DataSource;
import java.sql.Array;
import java.util.List;
import java.util.Set;

import static be.dnsbelgium.mercator.persistence.VisitRepository.getList;

@Component
public class TechnologyAnalyzerWebCrawlRepository {

    private static final Logger logger = LoggerFactory.getLogger(TechnologyAnalyzerWebCrawlRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final MeterRegistry meterRegistry;

    public TechnologyAnalyzerWebCrawlRepository(DataSource dataSource, MeterRegistry meterRegistry) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(dataSource);
        this.meterRegistry = meterRegistry;
        ApplicationConversionService applicationConversionService = new ApplicationConversionService();
        applicationConversionService.addConverter(new ArrayConvertor());
    }

    public void createTables() {
        String ddl_technology_analyzer_web_crawl_result = """
                create table if not exists technology_analyzer_web_crawl_result
                (
                    visit_id       varchar(26)              not null,
                    domain_name    varchar(255),
                    detected_technologies varchar[]
                )
                """;
        execute(ddl_technology_analyzer_web_crawl_result);
    }

    private void execute(String sql) {
        logger.debug("Start executing sql = {}", sql);
        jdbcTemplate.execute(sql);
        logger.debug("Done executing sql {}", sql);
    }

    public void saveTechnologyAnalyzerWebCrawlResult(@NotNull TechnologyAnalyzerWebCrawlResult crawlResult) {
        var sample = Timer.start(meterRegistry);
        String insert = """
                insert into technology_analyzer_web_crawl_result
                (
                    visit_id,
                    domain_name,
                    detected_technologies
                )
                values (?, ?, ?)
                """;
        Array detectedTechnologies = jdbcTemplate.execute(
                (ConnectionCallback<Array>) con -> con.createArrayOf("text",
                        crawlResult.getDetectedTechnologies().toArray()));
        int rowsInserted = jdbcTemplate.update(
                insert,
                crawlResult.getVisitId(),
                crawlResult.getDomainName(),
                detectedTechnologies);
        logger.debug("domain={} rowsInserted={}", crawlResult.getDomainName(), rowsInserted);
        sample.stop(meterRegistry.timer("repository.insert.technology.analyzer.web.crawl.result"));
    }

    public List<TechnologyAnalyzerWebCrawlResult> findTechnologyAnalyzerWebCrawlResults(String visitId) {
        return jdbcClient
                .sql("select * from technology_analyzer_web_crawl_result where visit_id = ?")
                .param(visitId)
                .query((rs, rowNum) -> {
                    String domainName = rs.getString("domain_name");
                    List<String> detectedTechnologies = getList(rs, "detected_technologies");

                    for (String detectedTechnology : detectedTechnologies) {
                        logger.debug("Detected technology: {}", detectedTechnology);
                    }

                    return TechnologyAnalyzerWebCrawlResult.builder()
                            .visitId(visitId)
                            .domainName(domainName)
                            .detectedTechnologies(Set.copyOf(detectedTechnologies))
                            .build();
                })
                .list();
    }

}