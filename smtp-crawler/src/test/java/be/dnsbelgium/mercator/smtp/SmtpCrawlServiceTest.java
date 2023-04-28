package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpVisitRepository;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
  properties =
    {
      "smtp.crawler.input.queue.name=smtp.queue",
      "smtp.crawler.initial-response-time-out=2s",
      "smtp.crawler.read-time-out=2s"
    })
@ActiveProfiles({"test", "local"})
class SmtpCrawlServiceTest {

  @Autowired
  SmtpCrawlService service;
  @Autowired
  SmtpVisitRepository repository;

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "smtp_crawler");
    localstack.setDynamicPropertySource(registry);
  }
  // This test does not really need SQS but when we don't start localstack, we get a stacktrace in the logs
  //  (Unable to execute HTTP request: Connect to localhost:4576 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed)
  // because starts listening on SQS in a background thread.
  // Our options
  // (a) ignore the warning + stacktrace
  // (b) find a way to not start listening on SQS
  // (c) start LocalStack from within this test
  //
  //  So far we have chosen for option (c) which adds 17 seconds to the execution time of this test.

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "smtp.queue");
  }

  private static final Logger logger = getLogger(SmtpCrawlServiceTest.class);

  @Test
  @Transactional
  public void integrationTest() throws Exception {
    UUID uuid = UUID.randomUUID();
    VisitRequest request = new VisitRequest(uuid, "dnsbelgium.be");
    SmtpVisitEntity visit = service.retrieveSmtpInfo(request);
    service.save(visit);
    Optional<SmtpVisitEntity> find = repository.findByVisitId(uuid);
    assertThat(find).isPresent();
    SmtpVisitEntity found = find.get();
    logger.info("found = {}", found);
    assertThat(found).isNotNull();
    assertThat(found.getDomainName()).isEqualTo(request.getDomainName());
    assertThat(found.getVisitId()).isEqualTo(uuid);
  }

  @Test
  void saveTest(){
    SmtpConversationEntity conversation1 = new SmtpConversationEntity();
    conversation1.setIp("1.2.3.4");
    conversation1.setIpVersion(4);

    SmtpConversationEntity conversation2 = new SmtpConversationEntity();
    conversation2.setIp("5.6.7.8");
    conversation2.setIpVersion(4);

    UUID visitId = UUID.randomUUID();
    SmtpVisitEntity visit = new SmtpVisitEntity();
    visit.setVisitId(visitId);
    visit.setDomainName("dnsbelgium.be");
    visit.setNumConversations(2);

    SmtpHostEntity host = new SmtpHostEntity();
    host.setConversation(conversation1);
    host.setHostName("protection.outlook.com");
    host.setPriority(0);
    host.setFromMx(true);
    host.setVisit(visit);

    SmtpHostEntity host2 = new SmtpHostEntity();
    host2.setConversation(conversation2);
    host2.setHostName("protection.outlook.com");
    host2.setPriority(0);
    host2.setFromMx(true);
    host2.setVisit(visit);

    List<SmtpHostEntity> hosts = new ArrayList<>();
    hosts.add(host);
    hosts.add(host2);

    visit.setHosts(hosts);

    service.save(visit);

    Optional<SmtpVisitEntity> savedVisit = repository.findByVisitId(visitId);
    assertThat(savedVisit).isPresent();
    SmtpVisitEntity visitEntity = savedVisit.get();
    assertThat(visitEntity.getDomainName()).isEqualTo("dnsbelgium.be");
  }
}
