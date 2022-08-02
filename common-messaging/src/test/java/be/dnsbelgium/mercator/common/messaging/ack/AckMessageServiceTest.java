package be.dnsbelgium.mercator.common.messaging.ack;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.queue.QueueClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({AckConfig.class})
@TestPropertySource(properties = {"crawler.ack.queue-name=test"})
class AckMessageServiceTest {

  public static final String DOMAIN_NAME = "test.be";
  public static final UUID VISIT_ID = UUID.randomUUID();

  @Autowired
  AckMessageService ackMessageService;

  @MockBean
  QueueClient queueClient;

  @MockBean
  MeterRegistry meterRegistry;

  @Test
  void sendAck() {
    Counter counter = mock(Counter.class);
    when(meterRegistry.counter(eq(AckMetric.SENT), any(), any())).thenReturn(counter);

    VisitRequest visitRequest = new VisitRequest(VISIT_ID, DOMAIN_NAME);
    ackMessageService.sendAck(visitRequest, CrawlerModule.MUPPETS);

    verify(meterRegistry).counter(AckMetric.SENT, "crawlerModule", CrawlerModule.MUPPETS.name());
    verify(counter).increment();
    verify(queueClient).convertAndSend("test", new AckCrawlMessage(VISIT_ID, DOMAIN_NAME, CrawlerModule.MUPPETS));
  }

}