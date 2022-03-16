package be.dnsbelgium.mercator.dispatcher.persistence;

import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.testcontainers.junit.jupiter.Container;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.slf4j.LoggerFactory.getLogger;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
public class DispatcherEventRepositoryTest {

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Autowired DispatcherEventRepository repository;
  @Autowired ApplicationContext context;

  private static final Logger logger = getLogger(DispatcherEventRepositoryTest.class);

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "dispatcher");
  }

  @Test
  public void insertDuplicate() {
    UUID visitId = UUID.randomUUID();
    DispatcherEvent dispatcherEvent1 = new DispatcherEvent(visitId, "abc.be", List.of());
    repository.save(dispatcherEvent1);
    logger.info("dispatcherEvent1 saved : = {}", dispatcherEvent1.getId());
    DispatcherEvent dispatcherEvent2 = new DispatcherEvent(visitId, "xxx.be", List.of());

    // To simulate a DispatcherEvent coming in with same visitId, we need to start a new transaction
    // (otherwise Hibernate throws a different type of Exception)

    assertThat(TestTransaction.isActive()).isTrue();
    TestTransaction.flagForCommit();
    TestTransaction.end();
    logger.info("first tx ended");

    TestTransaction.start();
    TestTransaction.flagForCommit();
    logger.info("Second tx started and flagged for commit");

    try {
      repository.save(dispatcherEvent2);
      logger.info("dispatcherEvent2 saved: {}", dispatcherEvent2.getId());
      logger.info("Finding all DispatcherEvent, this will trigger a flush and hence the insert that fails");
      repository.findAll();
      fail("Should have thrown DataIntegrityViolationException");

    } catch (DataIntegrityViolationException e) {
      logger.error("DataIntegrityViolationException", e);

      boolean duplicateKey = DispatcherEventRepository.exceptionContains(e, "duplicate key value violates unique constraint \"dispatcher_event_pkey\"");
      assertThat(duplicateKey).isTrue();

    } finally {
      logger.info("finally: rollback");
      TestTransaction.flagForRollback();
      TestTransaction.end();
    }
  }

  @Test
  public void addAcks() {
    // simulate an incoming DispatcherEvent followed by incoming ACKs for the same event
    // in different transactions
    // This would fail if DispatcherEvent.isNew() would always return true
    UUID visitId = UUID.randomUUID();
    DispatcherEvent dispatcherEvent = new DispatcherEvent(visitId, "abc.be", List.of());
    repository.save(dispatcherEvent);
    repository.findAll();
    TestTransaction.flagForCommit();
    TestTransaction.end();

    TestTransaction.start();
    dispatcherEvent.acks.put(CrawlerModule.DNS, ZonedDateTime.now());
    repository.save(dispatcherEvent);
    dispatcherEvent.acks.put(CrawlerModule.DNS, ZonedDateTime.now());
    repository.save(dispatcherEvent);
    TestTransaction.flagForCommit();
    TestTransaction.end();
  }

}
