package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import com.hubspot.smtp.client.ChannelClosedException;
import com.hubspot.smtp.client.SmtpClientResponse;
import com.hubspot.smtp.client.SmtpSessionConfig;
import com.hubspot.smtp.client.SmtpSessionFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.codec.smtp.DefaultSmtpRequest;
import io.netty.handler.codec.smtp.SmtpCommand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A stateful class to represents the conversation with one IP of an SMTP server.
 */
public class NioSmtpConversation implements be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConversation {

    private final SmtpConversation smtpConversation;
    private long connectionStart;
    private long connectionTimeMs = -1;
    private Temporal ehloSent;
    private Temporal ehloResponseReceived;
    private Temporal quitSent;

    private final MeterRegistry meterRegistry;
    private final SmtpSessionFactory sessionFactory;
    private final SmtpSessionConfig sessionConfig;
    private final SmtpConfig config;

    // https://tools.ietf.org/html/rfc5321#section-4.5.3.1.5
    // The maximum total length of a reply line including the reply code and
    // the <CRLF> is 512 octets.  More information may be conveyed through
    // multiple-line replies.
    public static final int MAX_REPLY_LENGTH = 512;
    public static final int NO_RESPONSE_CODE = -1;

    // TODO: inject
    // private final CertificateProcessor certificateProcessor = new CertificateProcessor(false);

    private static final Logger logger = getLogger(NioSmtpConversation.class);

    public NioSmtpConversation(
            MeterRegistry meterRegistry,
            SmtpSessionFactory sessionFactory,
            SmtpSessionConfig sessionConfig,
            SmtpConfig config,
            InetAddress ipAddress) {
        this.meterRegistry = meterRegistry;
        this.sessionFactory = sessionFactory;
        this.sessionConfig = sessionConfig;
        this.config = config;
        this.smtpConversation = new SmtpConversation(ipAddress);
    }

    // for testing
    protected SmtpConversation getSmtpHostIp() {
        return smtpConversation;
    }

    // TODO: now that we decided to block on every SMTP conversation we can consider using another SMTP client library
    @Override
    public SmtpConversation talk() {
        try {
            return start().get();
        } catch (ExecutionException | InterruptedException e) {
            meterRegistry.counter(MetricName.COUNTER_CONVERSATION_FAILED).increment();
            logger.error("crawl failed: {}", e.getMessage());
            return null;
        }
    }

    public CompletableFuture<SmtpConversation> start() {
        logger.debug("Starting SMTP crawl of {}", smtpConversation.getIp());
        CompletionStage<SmtpClientResponse> connected = this.connect();
        logger.debug("connected = {}", connected);
        return
                this.connect()
                .thenCompose(this::sendEHLO)
                .thenCompose(this::startTLS)
                .thenCompose(this::tlsCompleted)
                .thenCompose(this::closeSession)
                .thenCompose(aVoid -> CompletableFuture.completedFuture(smtpConversation))
                .exceptionally(this::handleException)
                .toCompletableFuture();
    }

    protected CompletionStage<SmtpClientResponse> connect() {
        logger.debug("connecting to {} ...", sessionConfig.getRemoteAddress());
        this.connectionStart = System.currentTimeMillis();
        return sessionFactory.connect(sessionConfig);
    }

    protected CompletableFuture<SmtpClientResponse> sendEHLO(SmtpClientResponse response) {
        logger.debug("received banner: {}", response.toString());
        connectionTimeMs = System.currentTimeMillis() - connectionStart;
        meterRegistry.timer(MetricName.TIMER_SMTP_CONNECT).record(connectionTimeMs, TimeUnit.MILLISECONDS);
        smtpConversation.setConnectionTimeMs(connectionTimeMs);
        smtpConversation.setConnectOK(!response.containsError());
        final String banner = StringUtils.abbreviate(response.toString(), MAX_REPLY_LENGTH);
        smtpConversation.setBanner(banner);
        logger.debug("Connected to {}. connectOK={}", smtpConversation.getIp(), smtpConversation.isConnectOK());
        smtpConversation.setConnectReplyCode(getReplyCode(response));
        DefaultSmtpRequest ehloCommand = new DefaultSmtpRequest(SmtpCommand.EHLO, config.getEhloDomain());
        ehloSent = now();
        return response.getSession().send(ehloCommand);
    }

    protected CompletableFuture<SmtpClientResponse> startTLS(SmtpClientResponse response) {
        logger.debug("EHLO response received: {}", response);
        ehloResponseReceived = now();
        meterRegistry.timer(MetricName.TIMER_EHLO_RESPONSE_RECEIVED).record(Duration.between(ehloSent, ehloResponseReceived));
        smtpConversation.setSupportedExtensions(response.getSession().getEhloResponse().getSupportedExtensions());
        return response.getSession().startTls();
    }

    private CompletableFuture<SmtpClientResponse> tlsCompleted(SmtpClientResponse response) {
        logger.debug("STARTTLS: {}", response);
        meterRegistry.timer(MetricName.TIMER_STARTTLS_COMPLETED).record(Duration.between(ehloResponseReceived, now()));
        smtpConversation.setStartTlsReplyCode(getReplyCode(response));
        logger.debug("session is encrypted: {}", response.getSession().isEncrypted());
        // response.getSession().getSSLSession().ifPresent(certificateProcessor::process);
        smtpConversation.setStartTlsOk(response.getSession().isEncrypted());
        return sendQuit(response);
    }

    private CompletableFuture<SmtpClientResponse> sendQuit(SmtpClientResponse response) {
        logger.debug("sending QUIT");
        quitSent = now();
        return response.getSession().send(new DefaultSmtpRequest(SmtpCommand.QUIT));
    }

    private CompletableFuture<Void> closeSession(SmtpClientResponse smtpClientResponse) {
        logger.debug("closing SMTP session with {}", sessionConfig.getRemoteAddress());
        meterRegistry.timer(MetricName.TIMER_SESSION_QUIT).record(Duration.between(quitSent, now()));
        return smtpClientResponse.getSession().close();
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * Extract the response code from the first response.
     *
     *    https://tools.ietf.org/html/rfc5321#section-4.2.1
     *    In a multiline reply, the reply code on each of the lines MUST be the
     *    same.  It is reasonable for the client to rely on this, so it can
     *    make processing decisions based on the code in any line, assuming
     *    that all others will be the same
     *
     * @param smtpClientResponse a SmtpClientResponse
     * @return the response code of the first response (or -1)
     */
    private int getReplyCode(SmtpClientResponse smtpClientResponse) {
        return (smtpClientResponse.getResponses().isEmpty() ? NO_RESPONSE_CODE : smtpClientResponse.getResponses().get(0).code());
    }

    private SmtpConversation handleException(Throwable throwable) {
        Instant start = now();
        // also set time to connect in case of Connection time-out => allows us to see how long we waited
        if (connectionTimeMs == -1) {
            connectionTimeMs = System.currentTimeMillis() - connectionStart;
            smtpConversation.setConnectionTimeMs(connectionTimeMs);
        }
        Throwable rootCause = rootCause(throwable);
        if (rootCause instanceof ChannelClosedException) {
            smtpConversation.setErrorMessage("channel was closed while waiting for response");
        } else {
            if (config.isLogStackTraces()) {
                logger.debug("exception: ", throwable);
            }
            String errorMessage = rootCause.getMessage();
            if (errorMessage == null) {
                errorMessage = rootCause.getClass().getSimpleName();
            }
            logger.debug("Conversation with {} failed: {}", sessionConfig.getRemoteAddress(), errorMessage);
            smtpConversation.setErrorMessage(errorMessage);
        }
        logger.debug("crawl done: smtpHostIp = {}", smtpConversation);
        meterRegistry.timer(MetricName.TIMER_HANDLE_CONVERSATION_EXCEPTION).record(Duration.between(start, now()));
        return smtpConversation;
    }
}
