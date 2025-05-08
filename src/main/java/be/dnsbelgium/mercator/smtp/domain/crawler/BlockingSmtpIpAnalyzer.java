package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import apache.commons.net.smtp.SMTPReply;
import apache.commons.net.smtp.SMTPSClient;
import apache.commons.net.util.TrustManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static apache.commons.net.smtp.SMTPCommand.EHLO;
import static apache.commons.net.util.SSLContextUtils.createSSLContext;
import static be.dnsbelgium.mercator.smtp.metrics.MetricName.TIMER_IP_CRAWL;

public class BlockingSmtpIpAnalyzer implements SmtpIpAnalyzer {

  private final MeterRegistry meterRegistry;
  private final GeoIPService geoIPService;
  private final SmtpConfig smtpConfig;
  private final SSLContext sslContext;

  private static final Logger logger = LoggerFactory.getLogger(BlockingSmtpIpAnalyzer.class);

  // https://tools.ietf.org/html/rfc5321#section-4.5.3.1.5
  // The maximum total length of a reply line including the reply code and
  // the <CRLF> is 512 octets.  More information may be conveyed through
  // multiple-line replies.
  public static final int MAX_REPLY_LENGTH = 512;


  @SneakyThrows
  public BlockingSmtpIpAnalyzer(MeterRegistry meterRegistry, SmtpConfig smtpConfig, GeoIPService geoIPService) {
    this.meterRegistry = meterRegistry;
    this.geoIPService = geoIPService;
    this.smtpConfig = smtpConfig;
    TrustManager trustManager = TrustManagerUtils.getAcceptAllTrustManager();
    sslContext = createSSLContext("TLS", null, new TrustManager[]{trustManager});
  }

  @Override
  public SmtpConversation crawl(InetAddress ip) {
    return meterRegistry.timer(TIMER_IP_CRAWL).record(() -> doCrawl(ip));
  }

  @SneakyThrows
  public SmtpConversation doCrawl(InetAddress ip) {
    SMTPSClient client = new SMTPSClient(sslContext);
    client.setConnectTimeout(smtpConfig.getInitialResponseTimeOutInMillis());
    client.setDefaultTimeout(smtpConfig.getInitialResponseTimeOutInMillis());
    logger.debug("crawling {}", ip);

    SmtpConversation.SmtpConversationBuilder conversationBuilder = SmtpConversation.builder();
    conversationBuilder.timestamp(Instant.now());
    conversationBuilder.ip(ip.getHostAddress());
    if (ip instanceof Inet4Address) {
      conversationBuilder.ipVersion(4);
    }
    if (ip instanceof Inet6Address) {
      conversationBuilder.ipVersion(6);
    }

    if (connect(client, ip, conversationBuilder)) {
      client.setDefaultTimeout(smtpConfig.getReadTimeOutInMillis());
      if (sendEHLO(client, conversationBuilder)) {
        startTLS(client, conversationBuilder);
      }
    }
    SmtpConversation conversation = conversationBuilder.build();
    geoIP(conversation);
    return conversation;
  }

  private boolean connect(SMTPSClient client, InetAddress ip, SmtpConversation.SmtpConversationBuilder conversationBuilder) {
    long start = System.currentTimeMillis();
    try {
      client.connect(ip, smtpConfig.getSmtpPort());
      long millis = System.currentTimeMillis() - start;
      conversationBuilder.connectionTimeMs(millis);

      int connectReplyCode = client.getReplyCode();
      conversationBuilder.connectReplyCode(connectReplyCode);
      conversationBuilder.connectOK(connectReplyCode < 400);
      final String banner = StringUtils.abbreviate(client.getReplyString().trim(), MAX_REPLY_LENGTH);
      conversationBuilder.banner(banner);
      if (!SMTPReply.isPositiveCompletion(connectReplyCode)) {
        logger.info("SMTP server at {} refused connection: {}", ip, client.getReplyString());
        conversationBuilder.errorMessage(client.getReplyString());
        conversationBuilder.error(Error.UNEXPECTED_REPLY_CODE);
        client.disconnect();
        return false;
      }
      client.setSoTimeout(smtpConfig.getReadTimeOutInMillis());
      return true;

    } catch (SocketTimeoutException e) {
      return error(conversationBuilder, Error.TIME_OUT, e);
    } catch (NoRouteToHostException e) {
      return error(conversationBuilder, Error.HOST_UNREACHABLE, e);
    } catch (IOException e) {
      return error(conversationBuilder, Error.CONNECTION_ERROR, e);
    } finally {
      long millis = System.currentTimeMillis() - start;
      conversationBuilder.connectionTimeMs(millis);
      meterRegistry.timer(MetricName.TIMER_SMTP_CONNECT).record(millis, TimeUnit.MILLISECONDS);
    }
  }

  private boolean error(SmtpConversation.SmtpConversationBuilder conversationBuilder, Error error, Exception e) {
    conversationBuilder.connectOK(false);
    conversationBuilder.errorMessage(e.getMessage());
    conversationBuilder.error(error);
    return false;
  }

  private boolean sendEHLO(SMTPSClient client, SmtpConversation.SmtpConversationBuilder conversationBuilder) {
    long start = System.currentTimeMillis();
    try {
      client.sendCommand(EHLO, smtpConfig.getEhloDomain());
      Set<String> extensions = extractExtensions(client.getReplyStrings());
      conversationBuilder.supportedExtensions(extensions);
      return true;
    } catch (IOException e) {
      conversationBuilder.errorMessage(e.getMessage());
      meterRegistry.counter(MetricName.COUNTER_CONVERSATION_FAILED).increment();
      return false;
    } finally {
      long millis = System.currentTimeMillis() - start;
      meterRegistry.timer(MetricName.TIMER_EHLO_RESPONSE_RECEIVED).record(millis, TimeUnit.MILLISECONDS);
    }
  }

  private void startTLS(SMTPSClient client, SmtpConversation.SmtpConversationBuilder conversationBuilder) {
    long start = System.currentTimeMillis();
    try {
      boolean startTlsOk = client.execTLS();
      int startTlsReplyCode = client.getReplyCode();
      conversationBuilder.startTlsReplyCode(startTlsReplyCode);
      conversationBuilder.startTlsOk(startTlsOk);
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
      conversationBuilder.startTlsOk(false);
      conversationBuilder.errorMessage(e.getMessage());
      conversationBuilder.error(Error.TLS_ERROR);
    } finally {
      long millis = System.currentTimeMillis() - start;
      meterRegistry.timer(MetricName.TIMER_STARTTLS_COMPLETED).record(millis, TimeUnit.MILLISECONDS);
    }
  }

  private Set<String> extractExtensions(String[] replyStrings) {
    List<String> extensions = List.of(replyStrings);
    return extensions.stream()
            .map(s -> StringUtils.removeStart(s, "250-"))
            .map(s -> StringUtils.removeStart(s, "250 "))
            .map(StringUtils::trim)
            .collect(Collectors.toSet());
  }


  private void geoIP(SmtpConversation smtpConversation) {
    Optional<Pair<Long, String>> asn = geoIPService.lookupASN(smtpConversation.getIp());
    if (asn.isPresent()) {
      smtpConversation.setAsn(asn.get().getKey());
      smtpConversation.setAsnOrganisation(asn.get().getValue());
    }
    Optional<String> country = geoIPService.lookupCountry(smtpConversation.getIp());
    country.ifPresent(smtpConversation::setCountry);
  }

}
