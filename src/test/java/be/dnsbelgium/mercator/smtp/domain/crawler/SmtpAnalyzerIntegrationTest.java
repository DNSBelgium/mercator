package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.CrawlStatus;
import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import be.dnsbelgium.mercator.MercatorApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

// This test is slow (and brittle since it relies on external state)
@EnabledIfEnvironmentVariable(named = "SMTP_INTEGRATION_TEST_ENABLED", matches = "true")
@SpringBootTest(classes = { MercatorApplication.class } )
@ActiveProfiles("test")
class SmtpAnalyzerIntegrationTest {

  @Autowired
  SmtpAnalyzer smtpAnalyzer;
  private static final Logger logger = getLogger(SmtpAnalyzerIntegrationTest.class);


  @Test
  public void dnsbelgium() throws Exception {
    logger.info("cachingSmtpCrawler = {}", smtpAnalyzer);
    var result = smtpAnalyzer.analyze("dnsbelgium.be");
    logger.info("dnsbelgium.be => {}", result);
    assertThat(result).isNotNull();
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(result.getCrawlStarted()).isNotNull();
    List<SmtpHost> hosts = result.getHosts();
    assertThat(hosts.size()).isGreaterThan(0);
    assertThat(hosts.getFirst().getConversations().getFirst().getConnectReplyCode()).isEqualTo(220);
    assertThat(hosts.getFirst().getConversations().getFirst().getStartTlsReplyCode()).isEqualTo(220);
    assertThat(hosts.getFirst().getConversations().getFirst().getConnectionTimeMs()).isGreaterThan(1);
    assertThat(hosts.getFirst().getConversations().getFirst().isConnectOK()).isTrue();
    assertThat(hosts.getFirst().getConversations().getFirst().isStartTlsOk()).isTrue();
    assertThat(hosts.getFirst().getConversations().getFirst().getErrorMessage()).isNull();
  }

}
