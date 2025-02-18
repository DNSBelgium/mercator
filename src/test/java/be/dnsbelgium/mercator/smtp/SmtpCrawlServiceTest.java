package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.geoip.DisabledGeoIPService;
import be.dnsbelgium.mercator.smtp.domain.crawler.*;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

class SmtpCrawlServiceTest {

  MeterRegistry meterRegistry = new SimpleMeterRegistry();
  SmtpConversationFactory conversationFactory = new NioSmtpConversationFactory(meterRegistry, SmtpConfig.testConfig(25));
  SmtpIpAnalyzer ipAnalyzer = new DefaultSmtpIpAnalyzer(meterRegistry, conversationFactory, new DisabledGeoIPService());
  MxFinder mxFinder = new MxFinder("8.8.8.8", 2, 10*1000, true);
  SmtpConversationCache conversationCache = new SmtpConversationCache(meterRegistry);
  SmtpAnalyzer analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, conversationCache, false, true, 10);

  private static final Logger logger = LoggerFactory.getLogger(SmtpCrawlServiceTest.class);

  SmtpCrawlServiceTest() throws NoSuchAlgorithmException, KeyManagementException, UnknownHostException {
  }

  @Test
  @Disabled
  public void integrationTest() {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, conversationCache, false, true, 10);
    SmtpVisit result = analyzer.visit("dnsbelgium.be");
    logger.info("result = {}", result);
    // this is basically the same as  SmtpAnalyzerIntegrationTest but without using Spring
  }



}
