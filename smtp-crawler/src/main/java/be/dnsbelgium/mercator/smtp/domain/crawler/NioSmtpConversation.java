package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.Error;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A stateful class to represents the conversation with one IP of an SMTP server.
 */
public class NioSmtpConversation implements ISmtpConversation {

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
    protected SmtpConversation getConversation() {
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
            smtpConversation.setError(Error.CHANNEL_CLOSED);
        } else {
            if (config.isLogStackTraces()) {
                logger.debug("exception: ", throwable);
            }
            String errorMessage = rootCause.getMessage();
            if (errorMessage == null) {
                errorMessage = rootCause.getClass().getSimpleName();
            }
            logger.debug("Conversation with {} failed: {}", sessionConfig.getRemoteAddress(), errorMessage);
            errorMessage = cleanErrorMessage(errorMessage);
            smtpConversation.setErrorMessage(errorMessage);
            smtpConversation.setError(getErrorFromErrorMessage(errorMessage));
        }
        logger.debug("crawl done: smtpConversation = {}", smtpConversation);
        meterRegistry.timer(MetricName.TIMER_HANDLE_CONVERSATION_EXCEPTION).record(Duration.between(start, now()));
        return smtpConversation;
    }

    public String cleanErrorMessage(String errorMessage){
        String cleanedErrorMessage;
        if (errorMessage.contains("connection timed out:")){
            cleanedErrorMessage = "Connection timed out";
        }
        else if (errorMessage.contains("Timed out waiting for a response to")){
            cleanedErrorMessage = "Timed out waiting for a response";
        }
        else if (errorMessage.contains("NotAfter:")){
            cleanedErrorMessage = "NotAfter";
        }
        else if (errorMessage.contains("Received fatal alert:")){
            cleanedErrorMessage = "Received fatal alert";
        }
        else if (errorMessage.contains("not an SSL/TLS record:")){
            cleanedErrorMessage = "Not an SSL/TLS record";
        }
        else if (errorMessage.contains("Received invalid line:")){
            cleanedErrorMessage = "Received invalid line";
        }
        else if (errorMessage.contains("The size of the handshake message")){
            cleanedErrorMessage = "Handshake message size exceeds maximum";
        }
        else if (errorMessage.contains("Usage constraint TLSServer check failed:")) {
            cleanedErrorMessage = "Usage constraint TLSServer check failed";
        }
        else {
            cleanedErrorMessage = errorMessage;
        }
        return cleanedErrorMessage;
    }

    public Error getErrorFromErrorMessage(String errorMessage){
        Error error;
        if (errorMessage.equals("Connection timed out") ||
          errorMessage.equals("Timed out waiting for a response")) {
            error = Error.TIME_OUT;
        }
        else if (errorMessage.equals("Connection reset by peer") ||
          errorMessage.equals("Connection refused") ||
          errorMessage.equals("Connection reset")) {
            error = Error.CONNECTION_ERROR;
        }
        else if (errorMessage.equals("conversation with loopback address skipped") ||
          errorMessage.equals("conversation with site local address skipped") ||
          errorMessage.equals("conversation with IPv6 SMTP host skipped") ||
          errorMessage.equals("conversation with IPv4 SMTP host skipped")) {
            error = Error.SKIPPED;
        }
        else if (errorMessage.equals("NotAfter") ||
          errorMessage.equals("Not an SSL/TLS record") ||
          errorMessage.equals("Usage constraint TLSServer check failed") ||
          errorMessage.equals("Empty issuer DN not allowed in X509Certificates") ||
          errorMessage.equals("Handshake message size exceeds maximum") ||
          errorMessage.matches("handshake timed out after .*") ||
          errorMessage.equals("unable to find valid certification path to requested target") ||
          errorMessage.matches("The server selected protocol version .* is not accepted by client preferences .*") ||
          errorMessage.equals("no more data allowed for version 1 certificate") ||
          errorMessage.matches("X.509 Certificate is incomplete:.*")) {
            error = Error.TLS_ERROR;
        }
        else if (errorMessage.equals("No route to host") ||
          errorMessage.equals("Network is unreachable") ||
          errorMessage.equals("Host is unreachable") ||
          errorMessage.equals("Network unreachable")){
            error = Error.HOST_UNREACHABLE;
        }
        else if (errorMessage.equals("ClosedChannelException") ||
          errorMessage.equals("channel was closed while waiting for response")){
            error = Error.CHANNEL_CLOSED;
        }
        else {
            error = Error.OTHER;
        }
        return error;
    }
}
