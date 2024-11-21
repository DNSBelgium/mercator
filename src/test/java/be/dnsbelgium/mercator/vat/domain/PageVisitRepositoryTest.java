package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.vat.crawler.persistence.PageVisit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@DataJpaTest
@ActiveProfiles({"local", "test"})
public class PageVisitRepositoryTest {


  private static final Logger logger = getLogger(PageVisitRepositoryTest.class);

  @Test
  @Commit
  public void insert() {
    String visitId = VisitIdGenerator.generate();
    PageVisit pageVisit = new PageVisit(
        visitId,
        "dnsbelgium.be",
        "http://www.dnsbelgium.be/contact",
        "/contact",
            TestUtils.now(),
            TestUtils.now().plusSeconds(123),
        200,
        "Wow, fancy website!",
        "<html><body><h1>Wow, fancy website!</h1></body></html>",
        List.of("BE-0466158640", "BE-0123455")
    );
    //pageVisitRepository.save(pageVisit);
    logger.info("pageVisit = {}", pageVisit);
    //assertThat(pageVisit.getId()).isNotNull();
    //Optional<PageVisit> found = pageVisitRepository.findById(pageVisit.getId());
    //assertThat(found).isPresent();
  }

  @Test
  public void save_0x00() {
    String visitId = VisitIdGenerator.generate();
    PageVisit pageVisit = new PageVisit(
        visitId,
        "just-a-test.be",
        "http://www.just-a-test.be/null-bytes",
        "/null-bytes-\u0000",
            TestUtils.now(),
            TestUtils.now().plusSeconds(123),
        200,
        "Wow, binary data with \u0000 bytes",
            "<html><body><h1>Wow, binary data with \u0000 bytes</h1></body></html>",
        List.of()
    );
    logger.info("This should NOT give a DataIntegrityViolationException");
    //pageVisitRepository.save(pageVisit);
    //logger.info("pageVisit = {}", pageVisit);
  }

}
