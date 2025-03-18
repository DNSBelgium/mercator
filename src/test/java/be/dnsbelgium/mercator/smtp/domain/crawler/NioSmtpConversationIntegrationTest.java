package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;

import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.ip;
import static be.dnsbelgium.mercator.smtp.domain.crawler.Mailpit.getMailPitContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class NioSmtpConversationIntegrationTest {

  private static final Logger logger = getLogger(NioSmtpConversationIntegrationTest.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @SneakyThrows
  @Test
  @EnabledIfEnvironmentVariable(named="smtp-integration-test", matches = "true")
  public void talk() {
    GenericContainer<?> mailpit = getMailPitContainer(true);
    mailpit.start();
    var smtpConfig = SmtpConfig.testConfig(mailpit.getMappedPort(1025));
    var conversationFactory = new NioSmtpConversationFactory(meterRegistry, smtpConfig);
    var ip = InetAddress.getLocalHost();
    NioSmtpConversation conversation = conversationFactory.create(ip);
    SmtpConversation smtpConversation = conversation.talk();
    logger.info("smtpConversation = {}", smtpConversation);
    assertThat(smtpConversation).isNotNull();
    assertThat(smtpConversation.isConnectOK()).isTrue();
    assertThat(smtpConversation.getSupportedExtensions()).isNotEmpty();
    assertThat(smtpConversation.getSupportedExtensions()).contains("SIZE 0", "ENHANCEDSTATUSCODES");
    assertThat(smtpConversation.getConnectReplyCode()).isEqualTo(220);
    assertThat(smtpConversation.getStartTlsReplyCode()).isEqualTo(220);
    assertThat(smtpConversation.getBanner()).contains("Mailpit ESMTP Service ready");
    assertThat(smtpConversation.getIp()).isNotEmpty();
    assertThat(smtpConversation.getIpVersion()).isEqualTo(4);
    assertThat(smtpConversation.isStartTlsOk()).isTrue();
    assertThat(smtpConversation.getConnectionTimeMs()).isBetween(1L, 500L);
    assertThat(smtpConversation.getErrorMessage()).isNull();
    assertThat(smtpConversation.getError()).isNull();
    assertThat(smtpConversation.getId()).isNull();
  }

  @SneakyThrows
  @Test
  @EnabledIfEnvironmentVariable(named="smtp-integration-test", matches = "true")
  public void noTls() {
    var mailpit = getMailPitContainer(false);
    mailpit.start();
    int port = mailpit.getMappedPort(1025);
    var smtpConfig = new SmtpConfig("x", 1, Duration.ofMinutes(3), Duration.ofMinutes(3), port, true, true);

    logger.info("container.getPortBindings = {}", mailpit.getPortBindings());
    var conversationFactory = new NioSmtpConversationFactory(meterRegistry, smtpConfig);
    var ip = InetAddress.getLocalHost();
    logger.info("ip = {}", ip);
    NioSmtpConversation conversation = conversationFactory.create(ip);
    SmtpConversation smtpConversation = conversation.talk();
    logger.info("smtpConversation = {}", smtpConversation);
    assertThat(smtpConversation).isNotNull();
    assertThat(smtpConversation.isConnectOK()).isTrue();
    assertThat(smtpConversation.getSupportedExtensions()).isNotEmpty();
    assertThat(smtpConversation.getSupportedExtensions()).contains("SIZE 0", "ENHANCEDSTATUSCODES");
    assertThat(smtpConversation.getConnectReplyCode()).isEqualTo(220);
    assertThat(smtpConversation.getStartTlsReplyCode()).isEqualTo(502);
    assertThat(smtpConversation.getBanner()).contains("Mailpit ESMTP Service ready");
    assertThat(smtpConversation.getIp()).isNotEmpty();
    assertThat(smtpConversation.getIpVersion()).isEqualTo(4);
    assertThat(smtpConversation.isStartTlsOk()).isFalse();
    assertThat(smtpConversation.getConnectionTimeMs()).isBetween(1L, 500L);
    assertThat(smtpConversation.getErrorMessage()).isNull();
    assertThat(smtpConversation.getError()).isNull();
    assertThat(smtpConversation.getId()).isNull();
  }

  @Test
  // set environment variable 'smtp-outbound-test' to 'true' to test an SMTP conversation with a server on the internet
  @EnabledIfEnvironmentVariable(named="smtp-outbound-test", matches = "true")
  public void outbound_test() throws KeyManagementException, NoSuchAlgorithmException, ExecutionException, InterruptedException {
    NioSmtpConversationFactory factory = new NioSmtpConversationFactory(new SimpleMeterRegistry(), SmtpConfig.testConfig());
    // this IP of "cavin.kuleuven.be" takes around 5.900 before accepting a connection
    NioSmtpConversation conversation = factory.create(ip("134.58.240.3"));
    logger.info("conversation = {}", conversation);
    long start = System.currentTimeMillis();
    CompletableFuture<SmtpConversation> result = conversation.start();
    logger.info("result = {}", result);
    logger.info("waiting until done ...");
    var smtpConversation = result.get();
    long millis = System.currentTimeMillis() - start;
    logger.info("Conversation took {} ms", millis);
    logger.info("smtpHostIp = {}", smtpConversation);
  }

}
