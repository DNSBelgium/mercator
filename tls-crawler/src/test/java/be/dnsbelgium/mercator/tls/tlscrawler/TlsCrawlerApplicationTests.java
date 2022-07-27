package be.dnsbelgium.mercator.tls.tlscrawler;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.test.LocalstackContainer;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "local"})
class TlsCrawlerApplicationTests {

  @Autowired
  TlsCrawler tlsCrawler;

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  @Container
  static LocalstackContainer localstack = new LocalstackContainer();

  @BeforeAll
  static void init() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "mercator-tls-crawler-input");
  }


  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "tls_crawler");
    localstack.setDynamicPropertySource(registry);
  }

  @Test
  void contextLoads() {
  }

  @Test
  void process() {
    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "google.be");
    tlsCrawler.process(visitRequest);
  }

  @Test
  void checkSSLAlgorithms() throws IOException, NoSuchAlgorithmException {

    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    SSLSocket soc = (SSLSocket) factory.createSocket();

    // Returns the names of the protocol versions which are
    // currently enabled for use on this connection.
    String[] protocols = soc.getEnabledProtocols();

    List<String> results = new ArrayList<>(List.of(protocols));

    SSLContext sslContext = SSLContext.getInstance("SSLv3");
    results.add(sslContext.getProtocol());

    assertThat(results).containsExactlyInAnyOrder(
        "TLSv1.3",
        "TLSv1.2",
        "TLSv1.1",
        "TLSv1",
        "SSLv3"
    );

    soc.close();
  }
}
