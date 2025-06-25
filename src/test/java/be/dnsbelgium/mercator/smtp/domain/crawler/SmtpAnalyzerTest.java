package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.CrawlStatus;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xbill.DNS.MXRecord;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.ip;
import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.mxRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

class SmtpAnalyzerTest {

  MxFinder mxFinder = mock(MxFinder.class);
  SmtpIpAnalyzer ipAnalyzer = mock(SmtpIpAnalyzer.class);
  SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  SmtpConversationCache conversationCache = new SmtpConversationCache(meterRegistry);

  SmtpAnalyzer analyzer;
  private static final String DOMAIN_NAME = "some-random-name.be";
  private static final Logger logger = getLogger(SmtpAnalyzerTest.class);

  InetAddress ip1 = ip("100.20.30.40");
  InetAddress ip2 = ip("100.20.30.44");
  InetAddress ip3 = ip("100.20.30.53");
  InetAddress ip4 = ip("100.20.30.54");
  InetAddress ipv6 = ip("2a02:1802:5f:fff1::16a");

  InetAddress localhost = ip("127.0.0.1");
  InetAddress privateIP = ip("172.16.2.3");

  SmtpConversation crawledIp1 = new SmtpConversation(ip1);
  SmtpConversation crawledIp2 = new SmtpConversation(ip2);
  SmtpConversation crawledIp3 = new SmtpConversation(ip3);
  SmtpConversation crawledIp4 = new SmtpConversation(ip4);
  SmtpConversation crawledIpv6 = new SmtpConversation(ipv6);

  MXRecord mx1 = mxRecord(DOMAIN_NAME, 10, "smtp1.name.be");
  MXRecord mx2 = mxRecord(DOMAIN_NAME, 20, "smtp2.name.be");

  @BeforeEach
  public void beforeEach() {
    analyzer = analyzer(false, false, 15);
    when(ipAnalyzer.crawl(ip1)).thenReturn(crawledIp1);
    when(ipAnalyzer.crawl(ip2)).thenReturn(crawledIp2);
    when(ipAnalyzer.crawl(ip3)).thenReturn(crawledIp3);
    when(ipAnalyzer.crawl(ip4)).thenReturn(crawledIp4);
    when(ipAnalyzer.crawl(ipv6)).thenReturn(crawledIpv6);
  }

  private SmtpAnalyzer analyzer(boolean skipIPv4, boolean skipIPv6, int maxHostsToContact) {
    return new SmtpAnalyzer(
        meterRegistry, ipAnalyzer, mxFinder, conversationCache
        , skipIPv4, skipIPv6, maxHostsToContact);
  }

  @Test
  public void noDnsRecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses();
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NO_REACHABLE_SMTP_SERVERS);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(0);
  }

  @Test
  public void noMxButOneARecord() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    assertThat(result.getHosts().getFirst().getConversations().size()).isEqualTo(1);
    SmtpHost server = result.getHosts().get(0);
    assertThat(server.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(server.getConversations()).isEqualTo(List.of(crawledIp1));
  }

  @Test
  public void noMxButTwoARecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    assertThat(result.getHosts().get(0).getConversations().size()).isEqualTo(3);
    var host1 = result.getHosts().get(0);
    assertThat(host1.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(host1.getConversations()).isEqualTo(List.of(crawledIp1, crawledIp2, crawledIpv6));
  }

  @Test
  public void noMxButOneARecordPriority0() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    assertThat(result.getHosts().get(0).getConversations().size()).isEqualTo(1);
    var host1 = result.getHosts().get(0);
    assertThat(host1.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(host1.getPriority()).isEqualTo(0);
    assertThat(host1.getConversations()).isEqualTo(List.of(crawledIp1));
  }

  @Test
  public void skipv6() throws Exception {
    analyzer = analyzer(false, true, 15);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    assertThat(result.getHosts().get(0).getConversations().size()).isEqualTo(3);
    var host1 = result.getHosts().get(0);
    assertThat(host1.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(host1.getConversations().stream().map(c -> c.getIp()))
        .isEqualTo(Arrays.asList(ip1.getHostAddress(), ip2.getHostAddress(), ipv6.getHostAddress()));
    assertThat(host1.getConversations().stream().map(c -> c.getErrorMessage())).isEqualTo(Arrays.asList(null, null, "conversation with IPv6 SMTP host skipped"));
  }

  @Test
  public void skipv4() throws Exception {
    analyzer = analyzer(true, false, 15);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    assertThat(result.getHosts().get(0).getConversations().size()).isEqualTo(3);
    var host1 = result.getHosts().get(0);
    assertThat(host1.getHostName()).isEqualTo(DOMAIN_NAME);

    assertThat(host1.getConversations().stream().map(c -> c.getErrorMessage()).toList()).isEqualTo(
        Arrays.asList("conversation with IPv4 SMTP host skipped",
            "conversation with IPv4 SMTP host skipped",
            null));
    assertThat(host1.getConversations().get(2)).isEqualTo(crawledIpv6);
  }

  @Test
  public void invalidHostName() throws Exception {
    expect(MxLookupResult.invalidHostName());
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.INVALID_HOSTNAME);
    assertThat(result.getHosts().size()).isEqualTo(0);
  }

  @Test
  public void queryFailed() throws Exception {
    expect(MxLookupResult.queryFailed());
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NETWORK_ERROR);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(0);
  }

  @Test
  public void oneMxWithOneIP() throws Exception {
    String mxTarget = mx1.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1)));
    expectIpAddresses(mxTarget, ip1);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    var host = result.getHosts().get(0);
    logger.info("host = {}", host);
    assertThat(host.getHostName()).isEqualTo(mxTarget);
    assertThat(host.getConversations().getFirst()).isEqualTo(crawledIp1);
  }

  @Test
  public void loopbackShouldBeSkipped() throws Exception {
    String mx1Target = mx1.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1)));
    expectIpAddresses(mx1Target, localhost, privateIP);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);

    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NO_REACHABLE_SMTP_SERVERS);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    var host = result.getHosts().get(0);
    logger.info("host = {}", host);
    assertThat(host.getConversations().size()).isEqualTo(2);
    assertThat(host.getHostName()).isEqualTo(mx1Target);
    assertThat(host.getConversations().stream().map(c -> c.getErrorMessage()).toList()).isEqualTo(Arrays.asList("conversation with loopback address skipped", "conversation with site local address skipped"));
  }

  @Test
  public void twoMxWithTwoIpEach() throws Exception {
    String mx1Target = mx1.getTarget().toString(true);
    String mx2Target = mx2.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1, mx2)));
    expectIpAddresses(mx1Target, ip1, ipv6);
    expectIpAddresses(mx2Target, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlStarted()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(2);
    var server1 = result.getHosts().get(0);
    var server2 = result.getHosts().get(1);
    logger.info("server = {}", server1);
    assertThat(server1.getHostName()).isEqualTo(mx1Target);
    assertThat(result.getHosts().get(0).getConversations()).isEqualTo(List.of(crawledIp1, crawledIpv6));
    logger.info("server = {}", server2);
    assertThat(server2.getHostName()).isEqualTo(mx2Target);
    assertThat(result.getHosts().get(1).getConversations()).isEqualTo(List.of(crawledIp2, crawledIpv6));
  }

  private void expectNoMxRecords() {
    when(mxFinder.findMxRecordsFor(DOMAIN_NAME)).thenReturn(MxLookupResult.noMxRecordsFound());
  }

  private void expect(MxLookupResult result) {
    when(mxFinder.findMxRecordsFor(DOMAIN_NAME)).thenReturn(result);
  }

  private void expectIpAddresses(InetAddress... ips) {
    when(mxFinder.findIpAddresses(DOMAIN_NAME)).thenReturn(Arrays.asList(ips));
  }

  private void expectIpAddresses(String hostName, InetAddress... ips) {
    when(mxFinder.findIpAddresses(hostName)).thenReturn(Arrays.asList(ips));
  }

  @Test
  public void maxHostsViaOneMxRecord() throws Exception {
    for (int maxHostsToContact : List.of(0, 1, 2, 3, 4)) {
      logger.info("max = {}", maxHostsToContact);
      analyzer = analyzer(false, true, maxHostsToContact);
      String mxTarget = mx1.getTarget().toString(true);
      expect(MxLookupResult.ok(List.of(mx1)));
      expectIpAddresses(mxTarget, ip1, ip2, ip3, ip4);
      var result = analyzer.analyze(DOMAIN_NAME);
      logger.info("result = {}", result);
      assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
      if (maxHostsToContact > 0)
        assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
      else
        assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NO_REACHABLE_SMTP_SERVERS);
      assertThat(result.getCrawlStarted()).isNotNull();
      assertThat(result.getHosts().size()).isEqualTo(Math.min(maxHostsToContact, 1));
      if (result.getHosts().size() > 0) {
        assertThat(result.getHosts().get(0).getConversations().size()).isEqualTo(4);
      }
    }
  }

  @Test
  public void maxHostsViaTwoMxRecords() throws Exception {
    for (int maxHostsToContact : List.of(0, 1, 2)) {
      logger.info("max = {}", maxHostsToContact);
      analyzer = analyzer(false, true, maxHostsToContact);
      String mxTarget1 = mx1.getTarget().toString(true);
      String mxTarget2 = mx2.getTarget().toString(true);
      expect(MxLookupResult.ok(List.of(mx1, mx2)));
      expectIpAddresses(mxTarget1, ip1, ip2);
      expectIpAddresses(mxTarget2, ip3, ip4);
      var result = analyzer.analyze(DOMAIN_NAME);
      logger.info("result = {}", result);
      assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
      if (maxHostsToContact > 0)
        assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
      else
        assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NO_REACHABLE_SMTP_SERVERS);
      assertThat(result.getCrawlStarted()).isNotNull();
      assertThat(result.getHosts().size()).isEqualTo(maxHostsToContact);
    }
  }

  @Test
  public void maxHostsViaAddressRecords() throws Exception {
    for (int maxHostsToContact = 0; maxHostsToContact <= 4; maxHostsToContact++) {
      analyzer = analyzer(false, true, maxHostsToContact);
      expectNoMxRecords();
      expectIpAddresses(ipv6, ip1, ip2, ip3, ip4);
      var result = analyzer.analyze(DOMAIN_NAME);
      logger.info("result = {}", result);
      assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
      assertThat(result.getHosts().size()).isEqualTo(Math.min(maxHostsToContact, 1));
      if (result.getHosts().size() > 0)
        assertThat(result.getHosts().get(0).getConversations().size()).isEqualTo(5);
    }
  }

}