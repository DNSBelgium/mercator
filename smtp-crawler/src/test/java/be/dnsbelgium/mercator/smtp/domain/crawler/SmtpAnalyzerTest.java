package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
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
  InetAddress ipv6 = ip("2a02:1802:5f:fff1::16a");

  InetAddress localhost = ip("127.0.0.1");
  InetAddress privateIP = ip("172.16.2.3");

  SmtpConversation crawledIp1 = new SmtpConversation(ip1);
  SmtpConversation crawledIp2 = new SmtpConversation(ip2);
  SmtpConversation crawledIpv6 = new SmtpConversation(ipv6);

  MXRecord mx1 = mxRecord(DOMAIN_NAME, 10, "smtp1.name.be");
  MXRecord mx2 = mxRecord(DOMAIN_NAME, 20, "smtp2.name.be");

  @BeforeEach
  public void beforeEach() {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, conversationCache, false, false);
    when(ipAnalyzer.crawl(ip1)).thenReturn(crawledIp1);
    when(ipAnalyzer.crawl(ip2)).thenReturn(crawledIp2);
    when(ipAnalyzer.crawl(ipv6)).thenReturn(crawledIpv6);
  }

  @Test
  public void noDnsRecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses();
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NO_REACHABLE_SMTP_SERVERS);
    assertThat(result.getTimestamp()).isNotNull();
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
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    SmtpHost server = result.getHosts().get(0);
    assertThat(server.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(server.getConversation()).isEqualTo(crawledIp1);
  }

  @Test
  public void noMxButTwoARecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(3);
    var host1 = result.getHosts().get(0);
    var host2 = result.getHosts().get(1);
    var host3 = result.getHosts().get(2);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host2.getHostName()).isEqualTo(ip2.getHostAddress());
    assertThat(host3.getHostName()).isEqualTo(ipv6.getHostAddress());
    assertThat(host1.getConversation()).isEqualTo(crawledIp1);
    assertThat(host2.getConversation()).isEqualTo(crawledIp2);
    assertThat(host3.getConversation()).isEqualTo(crawledIpv6);
  }

  @Test
  public void noMxButOneARecordPriority0() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    var host1 = result.getHosts().get(0);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host1.getPriority()).isEqualTo(0);
    assertThat(host1.getConversation()).isEqualTo(crawledIp1);
  }

  @Test
  public void skipv6() throws Exception {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, conversationCache, false, true);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(3);
    var host1 = result.getHosts().get(0);
    var host2 = result.getHosts().get(1);
    var host3 = result.getHosts().get(2);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host2.getHostName()).isEqualTo(ip2.getHostAddress());
    assertThat(host3.getHostName()).isEqualTo(ipv6.getHostAddress());
    assertThat(host1.getConversation()).isEqualTo(crawledIp1);
    assertThat(host2.getConversation()).isEqualTo(crawledIp2);
    assertThat(host3.getConversation().getIp()).isEqualTo(ipv6.getHostAddress());
    assertThat(host3.getConversation().getErrorMessage()).contains("conversation with IPv6 SMTP host skipped");
  }

  @Test
  public void skipv4() throws Exception {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, conversationCache, true, false);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    var result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(3);
    var host1 = result.getHosts().get(0);
    var host2 = result.getHosts().get(1);
    var host3 = result.getHosts().get(2);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host2.getHostName()).isEqualTo(ip2.getHostAddress());
    assertThat(host3.getHostName()).isEqualTo(ipv6.getHostAddress());
    assertThat(host1.getConversation().getErrorMessage()).isEqualTo("conversation with IPv4 SMTP host skipped");
    assertThat(host2.getConversation().getErrorMessage()).isEqualTo("conversation with IPv4 SMTP host skipped");
    assertThat(host3.getConversation()).isEqualTo(crawledIpv6);
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
    assertThat(result.getTimestamp()).isNotNull();
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
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    var host = result.getHosts().get(0);
    logger.info("host = {}", host);
    assertThat(host.getHostName()).isEqualTo(mxTarget);
    assertThat(host.getConversation()).isEqualTo(crawledIp1);
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
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(2);
    var host = result.getHosts().get(0);
    logger.info("host = {}", host);
    assertThat(host.getHostName()).isEqualTo(mx1Target);
    var conversation1 = result.getHosts().get(0).getConversation();
    var conversation2 = result.getHosts().get(1).getConversation();
    assertThat(conversation1.getErrorMessage()).isEqualTo("conversation with loopback address skipped");
    assertThat(conversation2.getErrorMessage()).isEqualTo("conversation with site local address skipped");

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
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(4);
    var server1 = result.getHosts().get(0);
    var server2 = result.getHosts().get(2);
    logger.info("server = {}", server1);
    assertThat(server1.getHostName()).isEqualTo(mx1Target);
    assertThat(result.getHosts().get(0).getConversation()).isEqualTo(crawledIp1);
    assertThat(result.getHosts().get(1).getConversation()).isEqualTo(crawledIpv6);
    logger.info("server = {}", server2);
    assertThat(server2.getHostName()).isEqualTo(mx2Target);
    assertThat(result.getHosts().get(2).getConversation()).isEqualTo(crawledIp2);
    assertThat(result.getHosts().get(3).getConversation()).isEqualTo(crawledIpv6);
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

}
