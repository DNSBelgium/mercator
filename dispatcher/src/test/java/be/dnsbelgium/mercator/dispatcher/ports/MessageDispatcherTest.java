package be.dnsbelgium.mercator.dispatcher.ports;

import be.dnsbelgium.mercator.common.messaging.dto.DispatcherRequest;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@TestPropertySource(properties = {
    "dispatcher.queue.in=dispatcher-input-test",
    "dispatcher.queue.out=dispatcher-output-test",
    "dispatcher.queue.ack=dispatcher-ack-test",
    "dispatcher.queues.forward=dispatcher-output1-test,dispatcher-output2-test",
})
class MessageDispatcherTest {

  public static final String DOMAIN_NAME = "test.be";

  @MockBean
  JmsTemplate jmsTemplate;

  @MockBean
  DispatcherEventRepository repository;

  @Captor
  ArgumentCaptor<String> queueCaptor;

  @Captor
  ArgumentCaptor<VisitRequest> requestCaptor;

  @Captor
  ArgumentCaptor<DispatcherEvent> eventCaptor;

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Autowired
  AmazonSQSClientBuilder sqsClientBuilder;

  private AmazonSQS sqs;
  private String queueUrl;

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "dispatcher");
    localstack.setDynamicPropertySource(registry);
  }

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "dispatcher-input-test");
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "dispatcher-ack-test");
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "dispatcher-output-test");
  }

  @BeforeEach
  void beforeEach() {
    sqs = sqsClientBuilder.build();
    queueUrl = sqs.getQueueUrl("dispatcher-input-test").getQueueUrl();
  }

  @Test
  void receiveDispatcherRequest() throws InterruptedException, JsonProcessingException {
    sqs.sendMessage(queueUrl, new ObjectMapper().writeValueAsString(new DispatcherRequest(UUID.randomUUID(), DOMAIN_NAME, Collections.emptyList())));

    Thread.sleep(1000); // Give a sec to leave the time for processing messages
    verify(jmsTemplate, times(2)).convertAndSend(queueCaptor.capture(), requestCaptor.capture());

    List<String> queues = queueCaptor.getAllValues();
    List<VisitRequest> requests = requestCaptor.getAllValues();

    assertThat(queues).isEqualTo(List.of("dispatcher-output1-test", "dispatcher-output2-test"));
    assertThat(requests).hasSize(2);

    for (VisitRequest request : requests) {
      assertThat(request).usingRecursiveComparison().ignoringFields("visitId").isEqualTo(new VisitRequest(UUID.randomUUID(), DOMAIN_NAME));
    }
  }

  @Test
  void testEventsAreStoredNoLabels() throws InterruptedException, JsonProcessingException {
    sqs.sendMessage(queueUrl, new ObjectMapper().writeValueAsString(new DispatcherRequest(UUID.randomUUID(), DOMAIN_NAME, Collections.emptyList())));

    Thread.sleep(1000); // Give a sec to leave the time for processing messages

    verify(repository).save(eventCaptor.capture());
    DispatcherEvent event = eventCaptor.getValue();
    assertThat(event.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(event.getLabels()).isEmpty();
  }

  @Test
  void testEventsAreStoredWithLabels() throws InterruptedException, JsonProcessingException {
    sqs.sendMessage(queueUrl, new ObjectMapper().writeValueAsString(new DispatcherRequest(UUID.randomUUID(), DOMAIN_NAME, List.of("coucou", "beuh"))));

    Thread.sleep(1000); // Give a sec to leave the time for processing messages

    verify(repository).save(eventCaptor.capture());
    DispatcherEvent event = eventCaptor.getValue();
    assertThat(event.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(event.getLabels()).containsExactly("coucou", "beuh");
  }

  @Test
  void testEventsAreStoredWithVisitId() throws InterruptedException, JsonProcessingException {
    UUID visitId = UUID.randomUUID();
    DispatcherRequest request = new DispatcherRequest(visitId, DOMAIN_NAME, List.of("coucou", "beuh"));
    sqs.sendMessage(queueUrl, new ObjectMapper().writeValueAsString(request));

    Thread.sleep(1000); // Give a sec to leave the time for processing messages

    verify(repository).save(eventCaptor.capture());
    DispatcherEvent event = eventCaptor.getValue();
    assertThat(event.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(event.getLabels()).containsExactly("coucou", "beuh");
    assertThat(event.getVisitId()).isEqualTo(visitId);
  }

}
