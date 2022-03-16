package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.vat.VatCrawlerService;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest
public class CrawlWebShops {

  @Autowired
  DataSource dataSource;

  @Autowired
  VatCrawlerService vatCrawlerService;

  private static final Logger logger = getLogger(CrawlWebShops.class);


  //@Test used locally to crawl VAT values for all e-commerce websites (for Senne)
  @Disabled
  public void test() throws InterruptedException {
    logger.info("dataSource = {}", dataSource);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    vatCrawlerService.setMaxVisitsPerDomain(20);
    vatCrawlerService.setPersistPageVisits(true);
    vatCrawlerService.setPersistBodyText(false);

    String user = jdbcTemplate.queryForObject("select user", String.class);
    logger.info("user = {}", user);

    ExecutorService executorService = Executors.newFixedThreadPool(60);

    AtomicInteger submitted = new AtomicInteger(0);
    AtomicInteger done = new AtomicInteger(0);

    vatCrawlerService.setPersistPageVisits(false);

    jdbcTemplate.query(
        "select visit_id, domain_name, url\n" +
            "from vat_crawler.ecommerce w\n" +
            "where not exists (select 1 from vat_crawler.vat_crawl_result r where r.visit_id = w.visit_id)\n" +
            "limit 20000\n",
        rs -> {
          String visitId    = rs.getString("visit_id");
          String domainName = rs.getString("domain_name");
          //String technology_slug = rs.getString("technology_slug");
          VisitRequest visitRequest = new VisitRequest(UUID.fromString(visitId), domainName);
          logger.info("visitRequest = {}", visitRequest);
          executorService.submit(
              () -> {
                vatCrawlerService.findVatValues(visitRequest);
                done.incrementAndGet();
              } );
          submitted.incrementAndGet();
        });

    logger.info("Submitted {} crawls", submitted);

    executorService.shutdown();

    while (!executorService.isTerminated()) {
      logger.info("Waiting until finished");
      logger.info("Done: {} of {} crawls", done, submitted);
      boolean terminated = executorService.awaitTermination(1, TimeUnit.MINUTES);
      logger.info("terminated = {}", terminated);
    }

    logger.info("jdbcTemplate = {}", jdbcTemplate);

  }

}
