package be.dnsbelgium.mercator.dispatcher.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckCrawlMessage;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import({AcknowledgeListener.class, CompositeMeterRegistry.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
class AcknowledgeListenerTest {

  public static final String DOMAIN_NAME = "test.be";

  @Autowired
  private DispatcherEventRepository repository;

  @Autowired
  private AcknowledgeListener acknowledgeListener;

  @MockBean
  QueueClient queueClient;

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void initContainers(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "dispatcher");
  }

  private UUID uuid;

  @BeforeEach
  void setUp() {
    uuid = UUID.randomUUID();
    DispatcherEvent event = new DispatcherEvent(uuid, DOMAIN_NAME, Collections.emptyList());
    repository.save(event);
    assertThat(repository.findById(uuid).isPresent()).isTrue();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void ack() {
    assertThat(repository.findById(uuid).get().getAcks().isEmpty()).isTrue();
    acknowledgeListener.ack(new AckCrawlMessage(uuid, DOMAIN_NAME, CrawlerModule.DNS));
    assertThat(repository.findById(uuid).get().getAcks().size()).isEqualTo(1);
    assertThat(repository.findById(uuid).get().getAcks()).containsOnlyKeys(CrawlerModule.DNS);
    verify(queueClient, never()).convertAndSend(anyString(), eq(new VisitRequest(uuid, DOMAIN_NAME)));

    acknowledgeListener.ack(new AckCrawlMessage(uuid, DOMAIN_NAME, CrawlerModule.SMTP));
    assertThat(repository.findById(uuid).get().getAcks().size()).isEqualTo(2);
    assertThat(repository.findById(uuid).get().getAcks()).containsOnlyKeys(CrawlerModule.DNS, CrawlerModule.SMTP);
    verify(queueClient, never()).convertAndSend(anyString(), eq(new VisitRequest(uuid, DOMAIN_NAME)));

    acknowledgeListener.ack(new AckCrawlMessage(uuid, DOMAIN_NAME, CrawlerModule.WAPPALYZER));
    assertThat(repository.findById(uuid).get().getAcks().size()).isEqualTo(3);
    assertThat(repository.findById(uuid).get().getAcks()).containsOnlyKeys(CrawlerModule.DNS, CrawlerModule.SMTP, CrawlerModule.WAPPALYZER);
    verify(queueClient, never()).convertAndSend(anyString(), eq(new VisitRequest(uuid, DOMAIN_NAME)));

    acknowledgeListener.ack(new AckCrawlMessage(uuid, DOMAIN_NAME, CrawlerModule.MUPPETS));
    assertThat(repository.findById(uuid).get().getAcks().size()).isEqualTo(4);
    assertThat(repository.findById(uuid).get().getAcks()).containsOnlyKeys(CrawlerModule.DNS, CrawlerModule.SMTP,
                                                                           CrawlerModule.WAPPALYZER, CrawlerModule.MUPPETS);

    acknowledgeListener.ack(new AckCrawlMessage(uuid, DOMAIN_NAME, CrawlerModule.TLS));
    assertThat(repository.findById(uuid).get().getAcks().size()).isEqualTo(5);
    assertThat(repository.findById(uuid).get().getAcks()).containsOnlyKeys(CrawlerModule.DNS, CrawlerModule.SMTP,
            CrawlerModule.WAPPALYZER, CrawlerModule.MUPPETS, CrawlerModule.TLS);

    if (CrawlerModule.VAT.isEnabled()) {
      acknowledgeListener.ack(new AckCrawlMessage(uuid, DOMAIN_NAME, CrawlerModule.VAT));
      assertThat(repository.findById(uuid).get().getAcks().size()).isEqualTo(6);
      assertThat(repository.findById(uuid).get().getAcks()).containsOnlyKeys(CrawlerModule.DNS, CrawlerModule.SMTP,
          CrawlerModule.WAPPALYZER, CrawlerModule.MUPPETS, CrawlerModule.TLS, CrawlerModule.VAT);
    }

    verify(queueClient).convertAndSend(anyString(), eq(new VisitRequest(uuid, DOMAIN_NAME)));
  }

  @Test
  void ackWrongVisitId() {
    UUID visitId = UUID.randomUUID();
    acknowledgeListener.ack(new AckCrawlMessage(visitId, DOMAIN_NAME, CrawlerModule.DNS));
    verify(queueClient, never()).convertAndSend(anyString(), any(VisitRequest.class));
    assertThat(repository.findById(visitId)).isEmpty();
  }
}