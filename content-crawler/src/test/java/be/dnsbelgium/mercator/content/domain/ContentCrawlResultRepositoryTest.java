package be.dnsbelgium.mercator.content.domain;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
class ContentCrawlResultRepositoryTest {

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @Autowired
  ContentCrawlResultRepository repository;

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "content_crawler");
  }

  @Test
  void findById() {
    ContentCrawlResult contentCrawlResult = ContentCrawlResultTest.contentCrawlResult();

    repository.save(contentCrawlResult);
    Optional<ContentCrawlResult> optional = repository.findById(contentCrawlResult.getId());
    assertThat(optional.isPresent()).isTrue();

    ContentCrawlResult retrieved = optional.get();
    assertThat(contentCrawlResult.getId()).isNotNull();
    assertThat(retrieved).isEqualTo(contentCrawlResult);
  }

  @Test
  public void findByVisitId() {
    UUID visitId = UUID.randomUUID();
    UUID visitId2 = UUID.randomUUID();
    ContentCrawlResult contentCrawlResult1 = ContentCrawlResultTest.contentCrawlResult(visitId, "http://www.dnsbelgium.be");
    ContentCrawlResult contentCrawlResult2 = ContentCrawlResultTest.contentCrawlResult(visitId, "https://www.dnsbelgium.be");
    ContentCrawlResult contentCrawlResult3 = ContentCrawlResultTest.contentCrawlResult(visitId2,"https://www.dnsbelgium.be");
    repository.save(contentCrawlResult1);
    repository.save(contentCrawlResult2);
    repository.save(contentCrawlResult3);
    List<ContentCrawlResult> foundCrawlResults = repository.findByVisitId(visitId);
    assertThat(foundCrawlResults.size()).isEqualTo(2);
    assertThat(foundCrawlResults).contains(contentCrawlResult1, contentCrawlResult2);
  }

  @Test
  public void ignoreDuplicates() {
    UUID visitId = UUID.randomUUID();
    ContentCrawlResult contentCrawlResult1 = ContentCrawlResultTest.contentCrawlResult(visitId);
    ContentCrawlResult contentCrawlResult2 = ContentCrawlResultTest.contentCrawlResult(visitId);
    contentCrawlResult1.setCrawl_succesfull(true);
    contentCrawlResult2.setCrawl_succesfull(false);
    boolean isDuplicate = repository.saveAndIgnoreDuplicateKeys(contentCrawlResult1);
    assertThat(isDuplicate).isFalse();
    isDuplicate = repository.saveAndIgnoreDuplicateKeys(contentCrawlResult2);
    assertThat(isDuplicate).isTrue();
  }

  @Test
  public void otherConstraintViolationsAreStillReported() {
    UUID visitId = UUID.randomUUID();
    ContentCrawlResult contentCrawlResult = ContentCrawlResultTest.contentCrawlResult(visitId);
    contentCrawlResult.setUrl(StringUtils.repeat("a", 400));
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndIgnoreDuplicateKeys(contentCrawlResult));
  }

  @Test
  public void testFindByVisitIdAndOk() {
    UUID visitId = UUID.randomUUID();
    String urls[] = { "http://url1.be", "http://url2.be", "http://url3.be"};
    List<ContentCrawlResult> found = repository.findByVisitIdAndOk(visitId, true);
    assertThat(found).isEmpty();
    ContentCrawlResult contentCrawlResult1 = ContentCrawlResultTest.contentCrawlResult(visitId, urls[0]);
    repository.save(contentCrawlResult1);
    found = repository.findByVisitIdAndOk(visitId, true);
    assertThat(found).hasSize(1);
    ContentCrawlResult contentCrawlResult2 = ContentCrawlResultTest.contentCrawlResult(visitId, urls[1]);
    contentCrawlResult2.setCrawl_succesfull(false);
    repository.save(contentCrawlResult2);
    found = repository.findByVisitIdAndOk(visitId, true);
    assertThat(found).hasSize(1);
    ContentCrawlResult contentCrawlResult3 = ContentCrawlResultTest.contentCrawlResult(visitId, urls[2]);
    repository.save(contentCrawlResult3);
    found = repository.findByVisitIdAndOk(visitId, true);
    assertThat(found).hasSize(2);
    assertThat(found).contains(contentCrawlResult1, contentCrawlResult3);
    found = repository.findByVisitIdAndOk(visitId, false);
    assertThat(found).hasSize(1);
    assertThat(found).contains(contentCrawlResult2);
  }

}
