package be.dnsbelgium.mercator.tls.tlscrawler;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class TlsCrawlerApplicationTests {

  @Autowired
  TlsCrawler tlsCrawler;

  @Test
  void contextLoads() {
  }

  @Test
  public void process() {
    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "google.be");
    tlsCrawler.process(visitRequest);
  }
}
