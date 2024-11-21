package be.dnsbelgium.mercator.vat.domain;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("since this test depends on internet access and on the state of the sites it tries to visit")
@SpringBootTest
@ActiveProfiles({"local", "test"})
public class VatCrawlerIntegrationTest {

  // TODO: remove or rewrite this class/test

  // Integration Test for locally debugging websites that lead to unexpected failures.
  // This test does not use mocks: it uses the real VatCrawler to visit websites
  // and stores the result in a Postgres database.
  // LocalStack is started to avoid complaints about unexisting SQS queues

//  @Autowired
//  private VatCrawler vatCrawler;
//
//  @Test
//  public void casino() {
//    vatCrawler.process(new VisitRequest("casino-legal-belgique.be"));
//  }

}