package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.domain.crawler.config.TestContext;
import be.dnsbelgium.mercator.smtp.persistence.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.SmtpCrawlResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

// "Do not run on Jenkins. This test is brittle since it relies on external state"
// @EnabledOnOs(OS.MAC)
@Disabled
@SpringJUnitConfig({
    TestContext.class,
    DefaultSmtpIpAnalyzer.class,
    NioSmtpConversationFactory.class,
    SmtpAnalyzer.class,
    MxFinder.class,
    SimpleMeterRegistry.class})
class SmtpAnalyzerIntegrationTest {

  @MockBean
  GeoIPService geoIPService;

  @Autowired
  SmtpAnalyzer smtpAnalyzer;
  private static final Logger logger = getLogger(SmtpAnalyzerIntegrationTest.class);

  @Test
  public void dnsbelgium() throws Exception {
    logger.info("cachingSmtpCrawler = {}", smtpAnalyzer);
    SmtpCrawlResult result = smtpAnalyzer.analyze("dnsbelgium.be");
    logger.info("result = {}", result);
    assertThat(result).isNotNull();
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isGreaterThan(0);
    assertThat(result.getServers().get(0).getHosts().size()).isGreaterThan(0);
    assertThat(result.getServers().get(0).getHosts().get(0).getConnectReplyCode()).isEqualTo(220);
    assertThat(result.getServers().get(0).getHosts().get(0).getConnectReplyCode()).isEqualTo(220);
    assertThat(result.getServers().get(0).getHosts().get(0).getStartTlsReplyCode()).isEqualTo(220);
    assertThat(result.getServers().get(0).getHosts().get(0).getConnectionTimeMs()).isGreaterThan(1);
    assertThat(result.getServers().get(0).getHosts().get(0).isConnectOK()).isTrue();
    assertThat(result.getServers().get(0).getHosts().get(0).isStartTlsOk()).isTrue();
    assertThat(result.getServers().get(0).getHosts().get(0).getErrorMessage()).isNull();
  }

  @Test
  public void abc() throws Exception {
    logger.info("cachingSmtpCrawler = {}", smtpAnalyzer);
    SmtpCrawlResult result = smtpAnalyzer.analyze("bosteels.eu");
    logger.info("result = {}", result);
    assertThat(result).isNotNull();
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    //assertThat(result.getDomainName()).isEqualTo("abc.be");
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isGreaterThan(0);
    assertThat(result.getServers().get(0).getHosts().size()).isGreaterThan(0);
    assertThat(result.getServers().get(0).getHosts().get(0).getConnectReplyCode()).isEqualTo(220);
    assertThat(result.getServers().get(0).getHosts().get(0).getConnectReplyCode()).isEqualTo(220);
    assertThat(result.getServers().get(0).getHosts().get(0).getStartTlsReplyCode()).isEqualTo(220);
    assertThat(result.getServers().get(0).getHosts().get(0).getConnectionTimeMs()).isGreaterThan(1);
    assertThat(result.getServers().get(0).getHosts().get(0).isConnectOK()).isTrue();
    assertThat(result.getServers().get(0).getHosts().get(0).isStartTlsOk()).isTrue();
    assertThat(result.getServers().get(0).getHosts().get(0).getErrorMessage()).isNull();
  }
}
