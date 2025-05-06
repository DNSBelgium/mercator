package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import apache.commons.net.smtp.SMTPConnectionClosedException;
import apache.commons.net.smtp.SMTPReply;
import apache.commons.net.smtp.SMTPSClient;
import apache.commons.net.util.TrustManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static apache.commons.net.smtp.SMTPCommand.EHLO;
import static apache.commons.net.util.SSLContextUtils.createSSLContext;

public class BlockingSmtpIpAnalyzer implements SmtpIpAnalyzer {

  private final MeterRegistry meterRegistry;
  private final GeoIPService geoIPService;
  private final SmtpConfig smtpConfig;
  private final SSLContext sslContext;

  private static final Logger logger = LoggerFactory.getLogger(BlockingSmtpIpAnalyzer.class);

  @SneakyThrows
  public BlockingSmtpIpAnalyzer(MeterRegistry meterRegistry, SmtpConfig smtpConfig, GeoIPService geoIPService) {
    this.meterRegistry = meterRegistry;
    this.geoIPService = geoIPService;
    this.smtpConfig = smtpConfig;
    TrustManager trustManager = TrustManagerUtils.getAcceptAllTrustManager();
    sslContext = createSSLContext("TLS", null, new TrustManager[]{trustManager});
  }

  @SneakyThrows
  @Override
  public SmtpConversation crawl(InetAddress ip) {
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
      // TODO: should we check the reply code or just that the TCP connection was set up ?
      conversationBuilder.connectOK(connectReplyCode < 400);
      conversationBuilder.banner(client.getReplyString().trim());
      if (!SMTPReply.isPositiveCompletion(connectReplyCode)) {
        logger.info("SMTP server refused connection: {}", client.getReplyString());
        conversationBuilder.errorMessage(client.getReplyString());
        conversationBuilder.error(Error.UNEXPECTED_REPLY_CODE);
        client.disconnect();
        return false;
      }
      client.setSoTimeout(smtpConfig.getReadTimeOutInMillis());
      return true;

    } catch (SocketTimeoutException e) {
      conversationBuilder.connectOK(false);
      conversationBuilder.errorMessage(e.getMessage());
      conversationBuilder.error(Error.TIME_OUT);
      return false;
    } catch (NoRouteToHostException e) {
      conversationBuilder.connectOK(false);
      conversationBuilder.errorMessage(e.getMessage());
      conversationBuilder.error(Error.HOST_UNREACHABLE);
      return false;
    } catch (IOException e) {
      conversationBuilder.connectOK(false);
      conversationBuilder.errorMessage(e.getMessage());
      conversationBuilder.error(Error.CONNECTION_ERROR);
      return false;
    } finally {
      long millis = System.currentTimeMillis() - start;
      conversationBuilder.connectionTimeMs(millis);
    }
  }

  private boolean sendEHLO(SMTPSClient client, SmtpConversation.SmtpConversationBuilder conversationBuilder) {
    try {
      client.sendCommand(EHLO, smtpConfig.getEhloDomain());
      Set<String> extensions = extractExtensions(client.getReplyStrings());
      conversationBuilder.supportedExtensions(extensions);
      return true;
    } catch (IOException e) {
      conversationBuilder.errorMessage(e.getMessage());
      return false;
    }
  }

  private void startTLS(SMTPSClient client, SmtpConversation.SmtpConversationBuilder conversationBuilder) {
    try {
      boolean startTlsOk = client.execTLS();
      int startTlsReplyCode = client.getReplyCode();
      conversationBuilder.startTlsReplyCode(startTlsReplyCode);
      conversationBuilder.startTlsOk(startTlsOk);

    } catch (SSLHandshakeException e) {
      logger.debug("SSLHandshakeException: {}", e.getMessage());
      conversationBuilder.startTlsOk(false);
      conversationBuilder.errorMessage(e.getMessage());
      conversationBuilder.error(Error.TLS_ERROR);
    } catch (IOException e) {
      logger.debug("IOException: {}", e.getMessage());
      conversationBuilder.startTlsOk(false);
      conversationBuilder.errorMessage(e.getMessage());
      conversationBuilder.error(Error.TLS_ERROR);
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
