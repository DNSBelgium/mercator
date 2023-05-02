package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
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
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(0);
  }

  @Test
  public void noMxButOneARecord() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1);
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    SmtpHostEntity server = result.getHosts().get(0);
    assertThat(server.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(server.getConversation().toSmtpConversation()).isEqualTo(crawledIp1);
  }

  @Test
  public void noMxButTwoARecords() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(3);
    SmtpHostEntity host1 = result.getHosts().get(0);
    SmtpHostEntity host2 = result.getHosts().get(1);
    SmtpHostEntity host3 = result.getHosts().get(2);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host2.getHostName()).isEqualTo(ip2.getHostAddress());
    assertThat(host3.getHostName()).isEqualTo(ipv6.getHostAddress());
    assertThat(host1.getConversation().toSmtpConversation()).isEqualTo(crawledIp1);
    assertThat(host2.getConversation().toSmtpConversation()).isEqualTo(crawledIp2);
    assertThat(host3.getConversation().toSmtpConversation()).isEqualTo(crawledIpv6);
  }

  @Test
  public void noMxButOneARecordPriority0() throws Exception {
    expectNoMxRecords();
    expectIpAddresses(ip1);
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    SmtpHostEntity host1 = result.getHosts().get(0);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host1.getPriority()).isEqualTo(0);
    assertThat(host1.getConversation().toSmtpConversation()).isEqualTo(crawledIp1);
  }

  @Test
  public void skipv6() throws Exception {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, false, true);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(3);
    SmtpHostEntity host1 = result.getHosts().get(0);
    SmtpHostEntity host2 = result.getHosts().get(1);
    SmtpHostEntity host3 = result.getHosts().get(2);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host2.getHostName()).isEqualTo(ip2.getHostAddress());
    assertThat(host3.getHostName()).isEqualTo(ipv6.getHostAddress());
    assertThat(host1.getConversation().toSmtpConversation()).isEqualTo(crawledIp1);
    assertThat(host2.getConversation().toSmtpConversation()).isEqualTo(crawledIp2);
    assertThat(host3.getConversation().toSmtpConversation().getIp()).isEqualTo(ipv6.getHostAddress());
    assertThat(host3.getConversation().toSmtpConversation().getErrorMessage()).contains("conversation with IPv6 SMTP host skipped");
  }

  @Test
  public void skipv4() throws Exception {
    analyzer = new SmtpAnalyzer(meterRegistry, ipAnalyzer, mxFinder, true, false);
    expectNoMxRecords();
    expectIpAddresses(ip1, ip2, ipv6);
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(3);
    SmtpHostEntity host1 = result.getHosts().get(0);
    SmtpHostEntity host2 = result.getHosts().get(1);
    SmtpHostEntity host3 = result.getHosts().get(2);
    assertThat(host1.getHostName()).isEqualTo(ip1.getHostAddress());
    assertThat(host2.getHostName()).isEqualTo(ip2.getHostAddress());
    assertThat(host3.getHostName()).isEqualTo(ipv6.getHostAddress());
    assertThat(host1.getConversation().toSmtpConversation().getErrorMessage()).isEqualTo("conversation with IPv4 SMTP host skipped");
    assertThat(host2.getConversation().toSmtpConversation().getErrorMessage()).isEqualTo("conversation with IPv4 SMTP host skipped");
    assertThat(host3.getConversation().toSmtpConversation()).isEqualTo(crawledIpv6);
  }

  @Test
  public void invalidHostName() throws Exception {
    expect(MxLookupResult.invalidHostName());
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.INVALID_HOSTNAME);
    assertThat(result.getHosts().size()).isEqualTo(0);
  }

  @Test
  public void queryFailed() throws Exception {
    expect(MxLookupResult.queryFailed());
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
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
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(1);
    SmtpHostEntity server = result.getHosts().get(0);
    logger.info("server = {}", server);
    assertThat(server.getHostName()).isEqualTo(mxTarget);
    assertThat(result.getHosts().get(0).getConversation().toSmtpConversation()).isEqualTo(crawledIp1);
  }

  @Test
  public void loopbackShouldBeSkipped() throws Exception {
    String mx1Target = mx1.getTarget().toString(true);
    expect(MxLookupResult.ok(List.of(mx1)));
    expectIpAddresses(mx1Target, localhost, privateIP);
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);

    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(2);
    SmtpHostEntity server1 = result.getHosts().get(0);
    logger.info("server = {}", server1);
    assertThat(server1.getHostName()).isEqualTo(mx1Target);

    SmtpConversationEntity host1 = result.getHosts().get(0).getConversation();
    SmtpConversationEntity host2 = result.getHosts().get(1).getConversation();

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
    SmtpVisitEntity result = analyzer.analyze(DOMAIN_NAME);
    logger.info("result = {}", result);
    assertThat(result.getDomainName()).isEqualTo(DOMAIN_NAME);
    assertThat(result.getCrawlStatus()).isEqualTo(CrawlStatus.OK);
    assertThat(result.getTimestamp()).isNotNull();
    assertThat(result.getHosts().size()).isEqualTo(4);
    SmtpHostEntity server1 = result.getHosts().get(0);
    SmtpHostEntity server2 = result.getHosts().get(2);
    logger.info("server = {}", server1);
    assertThat(server1.getHostName()).isEqualTo(mx1Target);
    assertThat(result.getHosts().get(0).getConversation().toSmtpConversation()).isEqualTo(crawledIp1);
    assertThat(result.getHosts().get(1).getConversation().toSmtpConversation()).isEqualTo(crawledIpv6);

    logger.info("server = {}", server2);
    assertThat(server2.getHostName()).isEqualTo(mx2Target);
    assertThat(result.getHosts().get(2).getConversation().toSmtpConversation()).isEqualTo(crawledIp2);
    assertThat(result.getHosts().get(3).getConversation().toSmtpConversation()).isEqualTo(crawledIpv6);
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
