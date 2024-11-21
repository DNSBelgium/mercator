package eu.bosteels.mercator.mono;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import be.dnsbelgium.mercator.test.TestUtils;
import eu.bosteels.mercator.mono.persistence.Repository;
import eu.bosteels.mercator.mono.persistence.TableCreator;
import eu.bosteels.mercator.mono.scheduling.CrawlRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static be.dnsbelgium.mercator.test.TestUtils.now;

@SuppressWarnings("SqlDialectInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test" })
@ContextConfiguration(classes = MonocatorApplication.class)
@Transactional
class RepositoryTest {

    @Autowired
    Repository repository;

    @Autowired
    TableCreator tableCreator;

    @Autowired DuckDataSource dataSource;

    private static final Logger logger = LoggerFactory.getLogger(RepositoryTest.class);

    @BeforeEach
    public void init() {
        tableCreator.init();
    }

    @Test
    public void findDone() {
        var list = repository.findDone("google.com");
        logger.info("list = {}", list);
    }

    @Test
    public void saveOperation() {
        repository.saveOperation(
                TestUtils.now(),
                "export database 'abc'",
                Duration.of(43, ChronoUnit.SECONDS)
        );
        // TODO: move to repository
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        var operations = jdbcTemplate.queryForList("select * from operations");
        logger.info("operations = {}", operations);
    }

    @Test
    @Disabled("first insert data")
    public void findDnsCrawlResultByVisitId() {
        Optional<DnsCrawlResult> crawlResult = repository.findDnsCrawlResultByVisitId_v2("8fc18d34-405a-442f-bb2a-9a157736af45");
        logger.info("crawlResult = {}", crawlResult);
        String prev_request_id = null;
        Request currentRequest = null;

        String prev_response_id = null;
        Response currentResponse = null;

        List<Request> results = new ArrayList<>();

        if (crawlResult.isPresent()) {
            for (Request request : crawlResult.get().getRequests()) {
                logger.info("* request = {}", request);
                boolean belongsToPreviousRequest = request.getId().equals(prev_request_id);
                if (!belongsToPreviousRequest) {
                    currentRequest = request;
                    prev_request_id = request.getId();
                    results.add(currentRequest);
                }
                for (Response response : request.getResponses()) {
                    if (belongsToPreviousRequest && response.getId() != null) {
                        var responses = new ArrayList<>(currentRequest.getResponses());
                        responses.add(response);
                        currentRequest.setResponses(responses);
                    }

                    boolean geoIpBelongsToPreviousResponse = Objects.equals(response.getId(), prev_response_id);
                    if (!geoIpBelongsToPreviousResponse) {
                        currentResponse = response;
                        prev_response_id = response.getId();
                    }
                    logger.info("  **  response = {}", response);
                    for (ResponseGeoIp geoIp : response.getResponseGeoIps()) {
                        logger.info("     *** geoIp = {}", geoIp);

                        if (geoIpBelongsToPreviousResponse) {
                            if (currentResponse != null) {
                                var geoIps = new ArrayList<>(currentResponse.getResponseGeoIps());
                                geoIps.add(geoIp);
                                currentResponse.setResponseGeoIps(geoIps);
                            }
                        }
                    }
                }
            }
        }
        logger.info("results = {}", results.size());

        boolean equal = crawlResult.isPresent() && results.equals(crawlResult.get().getRequests());
        logger.info("equal = {}", equal);
        // now reduce this list: group by responses if request.id is the same


    }

    @Test
    public void getRecentCrawlRates() {
        var rates = repository.getRecentCrawlRates(Repository.Frequency.PerHour, 100);
        for (CrawlRate rate : rates) {
            System.out.println("rate = " + rate);
        }
    }

    @Test
    public void getRecentCrawlRatesPerMinute() {
        var rates = repository.getRecentCrawlRates(Repository.Frequency.PerMinute, 100);
        for (CrawlRate rate : rates) {
            System.out.println("rate = " + rate);
        }
    }


    @Test
    public void timestamp() {
        var jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create or replace table ts (id int, ts timestamptz)");
        jdbcTemplate.execute("insert into ts (id, ts) values (1, current_timestamp )");
        jdbcTemplate.execute("insert into ts (id, ts) values (2, '2024-07-21 13:25:50.45+02' )");
        jdbcTemplate.update(
                "insert into ts (id, ts) values (?, ? )",
                3,
                "2024-07-21 13:25:50.45+02"
                );
        Instant dt = now();
        var local = dt.atZone(ZoneId.of("Europe/Brussels"));

        final long millis = dt.toEpochMilli();
        logger.info("millis = {}", millis);

        logger.info("dt    = {}", dt);
        logger.info("local = {}", local);

        jdbcTemplate.update(
                "insert into ts (id, ts) values (?, epoch_ms(?) )",
                ps -> {
                    ps.setLong(1, 4);
                    ps.setLong(2, 1000 * local.toEpochSecond());
                }
        );

        jdbcTemplate.update(
                "insert into ts (id, ts) values (?, ? )",
                ps -> {
                    ps.setLong(1, 60);
                    ps.setTimestamp(2, new Timestamp(millis));
                }
        );

        jdbcTemplate.update(
                "insert into ts (id, ts) values (?, ? )",
                70, new Timestamp(millis)
        );

        var list = jdbcTemplate.queryForList("select * from ts");
        //logger.info("list = {}", Arrays.to);
        for (Map<String, Object> objectMap : list) {
            logger.info(objectMap.toString());
        }


    }


}