package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.domain.crawler.config.TestContext;
import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

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
    SmtpVisit result = smtpAnalyzer.analyze("dnsbelgium.be");
    logger.info("result = {}", result);
    assertThat(result).isNotNull();
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(result.getTimestamp()).isNotNull();
    List<SmtpHost> hosts = result.getHosts();
    assertThat(hosts.size()).isGreaterThan(0);
    assertThat(hosts.get(0).getSmtpConversationEntity().getConnectReplyCode()).isEqualTo(220);
    assertThat(hosts.get(0).getSmtpConversationEntity().getStartTlsReplyCode()).isEqualTo(220);
    assertThat(hosts.get(0).getSmtpConversationEntity().getConnectionTimeMs()).isGreaterThan(1);
    assertThat(hosts.get(0).getSmtpConversationEntity().isConnectOK()).isTrue();
    assertThat(hosts.get(0).getSmtpConversationEntity().isStartTlsOk()).isTrue();
    assertThat(hosts.get(0).getSmtpConversationEntity().getErrorMessage()).isNull();
  }

  @Test
  public void abc() throws Exception {
    logger.info("cachingSmtpCrawler = {}", smtpAnalyzer);
    SmtpVisit result = smtpAnalyzer.analyze("bosteels.eu");
    logger.info("result = {}", result);
    assertThat(result).isNotNull();
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    //assertThat(result.getDomainName()).isEqualTo("abc.be");
    assertThat(result.getTimestamp()).isNotNull();
    List<SmtpHost> hosts = result.getHosts();
    assertThat(hosts.size()).isGreaterThan(0);
    assertThat(hosts.get(0).getSmtpConversationEntity().getConnectReplyCode()).isEqualTo(220);
    assertThat(hosts.get(0).getSmtpConversationEntity().getStartTlsReplyCode()).isEqualTo(220);
    assertThat(hosts.get(0).getSmtpConversationEntity().getConnectionTimeMs()).isGreaterThan(1);
    assertThat(hosts.get(0).getSmtpConversationEntity().isConnectOK()).isTrue();
    assertThat(hosts.get(0).getSmtpConversationEntity().isStartTlsOk()).isTrue();
    assertThat(hosts.get(0).getSmtpConversationEntity().getErrorMessage()).isNull();
  }
}
