package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.dto.SmtpServer;
import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpCrawlResult;
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
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, false, false);
    when(ipAnalyzer.crawl(ip1)).thenReturn(crawledIp1);
    when(ipAnalyzer.crawl(ip2)).thenReturn(crawledIp2);
    when(ipAnalyzer.crawl(ipv6)).thenReturn(crawledIpv6);
  }

  @Test
  public void noDnsRecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses();
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(0);
    assertThat(result.getCrawlTimestamp()).isNotNull();
  }

  @Test
  public void noMxButOneARecord() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(1);
    SmtpServer server = result.getServers().get(0);
    assertThat(server.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(server.getHosts().get(0)).isEqualTo(crawledIp1);
  }

  @Test
  public void noMxButTwoARecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(1);
    SmtpServer server = result.getServers().get(0);
    assertThat(server.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(server.getHosts().size()).isEqualTo(3);
    assertThat(server.getHosts().get(0)).isEqualTo(crawledIp1);
    assertThat(server.getHosts().get(1)).isEqualTo(crawledIp2);
    assertThat(server.getHosts().get(2)).isEqualTo(crawledIpv6);
  }

  @Test
  public void skipv6() throws Exception {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, false, true);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(1);
    SmtpServer server = result.getServers().get(0);
    assertThat(server.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(server.getHosts().size()).isEqualTo(3);
    assertThat(server.getHosts().get(0)).isEqualTo(crawledIp1);
    assertThat(server.getHosts().get(1)).isEqualTo(crawledIp2);
    assertThat(server.getHosts().get(2).getIp()).isEqualTo(ipv6.getHostAddress());
    assertThat(server.getHosts().get(2).getErrorMessage()).contains("conversation with IPv6 SMTP host skipped");
  }

  @Test
  public void skipv4() throws Exception {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, true, false);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(1);
    SmtpServer server = result.getServers().get(0);
    assertThat(server.getHostName()).isEqualTo(DOMAIN_NAME);
    assertThat(server.getHosts().size()).isEqualTo(3);
    assertThat(server.getHosts().get(0).getErrorMessage()).isEqualTo("conversation with IPv4 SMTP host skipped");
    assertThat(server.getHosts().get(1).getErrorMessage()).isEqualTo("conversation with IPv4 SMTP host skipped");
    assertThat(server.getHosts().get(2)).isEqualTo(crawledIpv6);
  }

  @Test
  public void invalidHostName() throws Exception {
    expect(MxLookupResult.invalidHostName());
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.INVALID_HOSTNAME);
    assertThat(result.getServers().size()).isEqualTo(0);
  }

  @Test
  public void queryFailed() throws Exception {
    expect(MxLookupResult.queryFailed());
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.NETWORK_ERROR);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(0);
  }

  @Test
  public void oneMxWithOneIP() throws Exception {
    String mxTarget = mx1.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1)));
    expectIpAddresses(mxTarget, ip1);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(1);
    SmtpServer server = result.getServers().get(0);
    logger.info("server = {}", server);
    assertThat(server.getHostName()).isEqualTo(mxTarget);
    assertThat(server.getHosts().size()).isEqualTo(1);
    assertThat(server.getHosts().get(0)).isEqualTo(crawledIp1);
  }

  @Test
  public void loopbackShouldBeSkipped() throws Exception {
    String mx1Target = mx1.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1)));
    expectIpAddresses(mx1Target, localhost, privateIP);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);

    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(1);
    SmtpServer server1 = result.getServers().get(0);
    logger.info("server = {}", server1);
    assertThat(server1.getHostName()).isEqualTo(mx1Target);
    assertThat(server1.getHosts().size()).isEqualTo(2);

    SmtpConversation host1 = server1.getHosts().get(0);
    SmtpConversation host2 = server1.getHosts().get(1);

    assertThat(host1.getErrorMessage()).isEqualTo("conversation with loopback address skipped");
    ;
    assertThat(host2.getErrorMessage()).isEqualTo("conversation with site local address skipped");
    ;

  }

  @Test
  public void twoMxWithTwoIpEach() throws Exception {
    String mx1Target = mx1.getTarget().toString(true);
    String mx2Target = mx2.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1, mx2)));
    expectIpAddresses(mx1Target, ip1, ipv6);
    expectIpAddresses(mx2Target, ip2, ipv6);
    SmtpCrawlResult result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getCrawlTimestamp()).isNotNull();
    assertThat(result.getServers().size()).isEqualTo(2);
    SmtpServer server1 = result.getServers().get(0);
    SmtpServer server2 = result.getServers().get(1);
    logger.info("server = {}", server1);
    assertThat(server1.getHostName()).isEqualTo(mx1Target);
    assertThat(server1.getHosts().size()).isEqualTo(2);
    assertThat(server1.getHosts().get(0)).isEqualTo(crawledIp1);
    assertThat(server1.getHosts().get(1)).isEqualTo(crawledIpv6);

    logger.info("server = {}", server2);
    assertThat(server2.getHostName()).isEqualTo(mx2Target);
    assertThat(server2.getHosts().size()).isEqualTo(2);
    assertThat(server2.getHosts().get(0)).isEqualTo(crawledIp2);
    assertThat(server2.getHosts().get(1)).isEqualTo(crawledIpv6);
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
