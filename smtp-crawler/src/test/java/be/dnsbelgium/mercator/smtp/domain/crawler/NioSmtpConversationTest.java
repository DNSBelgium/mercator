package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import com.hubspot.smtp.client.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.handler.codec.smtp.DefaultSmtpResponse;
import io.netty.handler.codec.smtp.SmtpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.ip;
import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

class NioSmtpConversationTest {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    SmtpSessionFactory sessionFactory;
    SmtpSessionConfig sessionConfig;
    SmtpSession session;

    private static final Logger logger = getLogger(NioSmtpConversationTest.class);

    @BeforeEach
    public void before() {
        sessionFactory = mock(SmtpSessionFactory.class);
        sessionConfig = SmtpSessionConfig.builder()
                .remoteAddress(InetSocketAddress.createUnresolved("500.600.700.800", 25))
                .build();
        session = mock(SmtpSession.class);
    }

    private NioSmtpConversation makeCrawl() {
        return new NioSmtpConversation(meterRegistry, sessionFactory, sessionConfig, SmtpConfig.testConfig(), ip("10.20.30.40"));
    }

    @Test
    public void testConnect() throws ExecutionException, InterruptedException {
        NioSmtpConversation crawl = makeCrawl();
        when(sessionFactory.connect(sessionConfig)).thenReturn(response(session, 123, "my-test-banner"));
        CompletableFuture<SmtpClientResponse> connect = crawl.connect().toCompletableFuture();
        connect.get();
        logger.info("crawl: " + crawl.getConversation());
        var result = crawl.getConversation();
        assertThat(result.getBanner()).isNull();
        assertThat(result.getErrorMessage()).isNullOrEmpty();
        assertThat(result.getIp()).isEqualTo("10.20.30.40");
        assertThat(result.getIpVersion()).isEqualTo(4);
        assertThat(result.isConnectOK()).isFalse();
        assertThat(result.getConnectReplyCode()).isEqualTo(0);
    }

    @Test
    public void sendEhlo() throws ExecutionException, InterruptedException {
        NioSmtpConversation crawl = makeCrawl();
        when(sessionFactory.connect(sessionConfig)).thenReturn(delayedResponse(40, session, 123, "my-test-banner"));
        when(session.send(any(SmtpRequest.class))).thenReturn(response(session, 100, "hi there, ehlo response"));
        CompletableFuture<SmtpClientResponse> response = sessionFactory.connect(sessionConfig);
        crawl.connect();
        crawl.sendEHLO(response.get());
        logger.info("crawl: " + crawl.getConversation());
        var result = crawl.getConversation();
        assertThat(result.getBanner()).isEqualTo("123 my-test-banner");
        assertThat(result.getErrorMessage()).isNullOrEmpty();
        assertThat(result.getIp()).isEqualTo("10.20.30.40");
        assertThat(result.getIpVersion()).isEqualTo(4);
        assertThat(result.isConnectOK()).isTrue();
        assertThat(result.getConnectReplyCode()).isEqualTo(123);
        // give it some leeway since sleep and java.lang.System.currentTimeMillis can use a granularity > 1ms
        assertThat(result.getConnectionTimeMs()).isBetween(10L, 70L);
    }

    @Test
    public void startTls() throws ExecutionException, InterruptedException {
        NioSmtpConversation crawl = makeCrawl();
        EhloResponse ehloResponse = EhloResponse.parse("hello.domain", List.of("abc", "def"), EnumSet.of(Extension.SIZE, Extension.PIPELINING));
        when(session.getEhloResponse()).thenReturn(ehloResponse);
        SmtpClientResponse connectResponse = response(session, 123, "my-test-banner").get();
        crawl.sendEHLO(connectResponse);
        crawl.startTLS(connectResponse);
        logger.info("crawl: " + crawl.getConversation());
        var result = crawl.getConversation();
        assertThat(result.getErrorMessage()).isNullOrEmpty();
        assertThat(result.getIp()).isEqualTo("10.20.30.40");
        assertThat(result.getIpVersion()).isEqualTo(4);
        assertThat(result.isConnectOK()).isTrue();
        assertThat(result.getSupportedExtensions()).contains("abc", "def");
        assertThat(result.getSupportedExtensions().size()).isEqualTo(2);
    }

    @Test
    public void connectionTimeOut() throws ExecutionException, InterruptedException {
        sessionConfig = sessionConfig
                .withInitialResponseTimeout(Duration.ofMillis(100))
                .withKeepAliveTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(3));
        NioSmtpConversation crawl = makeCrawl();
        CompletableFuture<SmtpClientResponse> future = new CompletableFuture<>();
        future.orTimeout(5, TimeUnit.MILLISECONDS);
        when(sessionFactory.connect(sessionConfig)).thenReturn(future);
        CompletableFuture<SmtpConversationEntity> crawlFuture = crawl.start();
        logger.info("crawl = {}", crawlFuture.get());
        assertThat(crawl.getConversation().getErrorMessage()).isNotBlank();
        assertThat(crawl.getConversation().getErrorMessage()).contains("TimeoutException");
    }

    @Test
    public void cleanErrorMessageTimedOutWaitingForResponse(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "[104.47.11.10]: Timed out waiting for a response to [initial response]";
        String cleanedErrorMessage = conversation.cleanErrorMessage(errorMessage);
        assertThat(cleanedErrorMessage).isEqualTo("Timed out waiting for a response");
    }

    @Test
    public void cleanErrorMessageNotAfter(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "NotAfter: Fri May 21 22:16:31 GMT 2021";
        String cleanedErrorMessage = conversation.cleanErrorMessage(errorMessage);
        assertThat(cleanedErrorMessage).isEqualTo("NotAfter");
    }

    @Test
    public void cleanErrorMessageReceivedFatalAlert(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "Received fatal alert: handshake_failure";
        String cleanedErrorMessage = conversation.cleanErrorMessage(errorMessage);
        assertThat(cleanedErrorMessage).isEqualTo("Received fatal alert");
    }

    @Test
    public void getErrorFromErrorMessageTimedOutWaitingForResponse(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "Timed out waiting for a response";
        Error error = conversation.getErrorFromErrorMessage(errorMessage);
        assertThat(error).isEqualTo(Error.TIME_OUT);
    }

    @Test
    public void getErrorFromErrorMessageNotAfter(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "NotAfter";
        Error error = conversation.getErrorFromErrorMessage(errorMessage);
        assertThat(error).isEqualTo(Error.TLS_ERROR);
    }

    @Test
    public void getErrorFromErrorMessageHostIsUnreachable(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "Host is unreachable";
        Error error = conversation.getErrorFromErrorMessage(errorMessage);
        assertThat(error).isEqualTo(Error.HOST_UNREACHABLE);
    }

    @Test
    public void getErrorFromErrorMessageConnectionRefused(){
        NioSmtpConversation conversation = makeCrawl();
        String errorMessage = "Connection refused";
        Error error = conversation.getErrorFromErrorMessage(errorMessage);
        assertThat(error).isEqualTo(Error.CONNECTION_ERROR);
    }

    private CompletableFuture<SmtpClientResponse> response(SmtpSession smtpSession, int responseCode, CharSequence... details) {
        return CompletableFuture.completedFuture(new SmtpClientResponse(smtpSession, new DefaultSmtpResponse(responseCode, details)));
    }

    @SuppressWarnings("SameParameterValue")
    private CompletableFuture<SmtpClientResponse> delayedResponse(long delayMs, SmtpSession smtpSession, int responseCode, CharSequence... details) {
        return CompletableFuture.supplyAsync(
                () -> {
                    sleep(delayMs);
                    return new SmtpClientResponse(smtpSession, new DefaultSmtpResponse(responseCode, details));
                });
    }

}
