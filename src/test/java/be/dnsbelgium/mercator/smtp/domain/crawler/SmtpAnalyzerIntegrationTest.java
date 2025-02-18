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
@EnabledIfEnvironmentVariable(named = "SMTP_TEST_ENABLED", matches = "True")
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
    assertThat(result.getTimestamp()).isNotNull();
    List<SmtpHost> hosts = result.getHosts();
    assertThat(hosts.size()).isGreaterThan(0);
    assertThat(hosts.get(0).getConversation().getConnectReplyCode()).isEqualTo(220);
    assertThat(hosts.get(0).getConversation().getStartTlsReplyCode()).isEqualTo(220);
    assertThat(hosts.get(0).getConversation().getConnectionTimeMs()).isGreaterThan(1);
    assertThat(hosts.get(0).getConversation().isConnectOK()).isTrue();
    assertThat(hosts.get(0).getConversation().isStartTlsOk()).isTrue();
    assertThat(hosts.get(0).getConversation().getErrorMessage()).isNull();
  }

}
