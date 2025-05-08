package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.DisabledGeoIPService;
import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import static be.dnsbelgium.mercator.smtp.domain.crawler.Mailpit.getMailPitContainer;
import static be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConfig.DEFAULT_EHLO_DOMAIN;
import static be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConfig.testConfig;
import static org.assertj.core.api.Assertions.assertThat;

class BlockingSmtpIpAnalyzerTest {

  private static final Logger logger = LoggerFactory.getLogger(BlockingSmtpIpAnalyzerTest.class);

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Test
  public void crawlWithTLS() {
    crawl(true);
  }

  @Test
  public void crawlWithoutTLS() {
    crawl(false);
  }

  @SneakyThrows
  private void crawl(boolean tlsEnabled) {
    GenericContainer<?> mailpit = getMailPitContainer(tlsEnabled);
    mailpit.start();
    logger.info("container.getPortBindings = {}", mailpit.getPortBindings());
    var ip = InetAddress.getLocalHost();
    logger.info("ip = {}", ip);
    SmtpConfig smtpConfig = SmtpConfig.testConfig(mailpit.getMappedPort(1025));
    BlockingSmtpIpAnalyzer smtpIpAnalyzer = new BlockingSmtpIpAnalyzer(meterRegistry, smtpConfig, new DisabledGeoIPService());
    SmtpConversation smtpConversation = smtpIpAnalyzer.crawl(ip);
    logger.info("smtpConversation = {}", smtpConversation);
    assertThat(smtpConversation).isNotNull();
    assertThat(smtpConversation.isConnectOK()).isTrue();
    assertThat(smtpConversation.getSupportedExtensions()).isNotEmpty();
    assertThat(smtpConversation.getSupportedExtensions()).contains("SIZE 0", "ENHANCEDSTATUSCODES");
    assertThat(smtpConversation.getConnectReplyCode()).isEqualTo(220);
    assertThat(smtpConversation.getBanner()).contains("Mailpit ESMTP Service ready");
    assertThat(smtpConversation.getIp()).isNotEmpty();
    assertThat(smtpConversation.getIpVersion()).isEqualTo(4);
    assertThat(smtpConversation.getConnectionTimeMs()).isBetween(1L, 500L);
    assertThat(smtpConversation.getErrorMessage()).isNull();
    assertThat(smtpConversation.getError()).isNull();
    assertThat(smtpConversation.isStartTlsOk()).isEqualTo(tlsEnabled);
    if (tlsEnabled) {
      assertThat(smtpConversation.getStartTlsReplyCode()).isEqualTo(220);
    } else {
      assertThat(smtpConversation.getStartTlsReplyCode()).isEqualTo(502);
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named="SMTP_INTEGRATION_TEST_ENABLED", matches = "true")
  public void timeOut() throws UnknownHostException {
    String ip = "52.101.68.8";
    SmtpConfig smtpConfig = new SmtpConfig(
            DEFAULT_EHLO_DOMAIN,
            Duration.ofSeconds(4),
            Duration.ofSeconds(8),
            25,
            true,
            true
    );
    InetAddress address = InetAddress.getByName(ip);
    BlockingSmtpIpAnalyzer smtpIpAnalyzer = new BlockingSmtpIpAnalyzer(meterRegistry, smtpConfig, new DisabledGeoIPService());
    SmtpConversation smtpConversationBlocking = smtpIpAnalyzer.crawl(address);
    smtpConversationBlocking.supportedExtensions = null;
    logger.info("smtpConversationBlocking = {}", smtpConversationBlocking);
    assertThat(smtpConversationBlocking).isNotNull();
    assertThat(smtpConversationBlocking.getConnectionTimeMs()).isGreaterThan(8000);
    assertThat(smtpConversationBlocking.getErrorMessage()).isEqualTo("Connect timed out");
    assertThat(smtpConversationBlocking.getError()).isEqualTo(Error.TIME_OUT);
  }

  @Test
  public void connectionRefused() throws UnknownHostException {
    SmtpConfig smtpConfig = testConfig();
    InetAddress address = InetAddress.getByName("127.0.0.1");
    BlockingSmtpIpAnalyzer smtpIpAnalyzer = new BlockingSmtpIpAnalyzer(meterRegistry, smtpConfig, new DisabledGeoIPService());
    SmtpConversation smtpConversationBlocking = smtpIpAnalyzer.crawl(address);
    smtpConversationBlocking.supportedExtensions = null;
    logger.info("smtpConversationBlocking = {}", smtpConversationBlocking);
    assertThat(smtpConversationBlocking).isNotNull();
    assertThat(smtpConversationBlocking.getConnectionTimeMs()).isLessThan(100);
    assertThat(smtpConversationBlocking.getErrorMessage()).isEqualTo("Connection refused");
    assertThat(smtpConversationBlocking.getError()).isEqualTo(Error.CONNECTION_ERROR);
  }

  @Test
  @EnabledIfEnvironmentVariable(named="SMTP_INTEGRATION_TEST_ENABLED", matches = "true")
  public void noRouteToHost() throws UnknownHostException {
    SmtpConfig smtpConfig = testConfig();
    InetAddress address = InetAddress.getByName("0.5.5.5");
    BlockingSmtpIpAnalyzer smtpIpAnalyzer = new BlockingSmtpIpAnalyzer(meterRegistry, smtpConfig, new DisabledGeoIPService());
    SmtpConversation smtpConversationBlocking = smtpIpAnalyzer.crawl(address);
    logger.info("smtpConversationBlocking = {}", smtpConversationBlocking);
    assertThat(smtpConversationBlocking).isNotNull();
    assertThat(smtpConversationBlocking.getErrorMessage()).isEqualTo("No route to host");
    assertThat(smtpConversationBlocking.getError()).isEqualTo(Error.HOST_UNREACHABLE);
    assertThat(smtpConversationBlocking.getConnectionTimeMs()).isLessThan(80);
  }

}