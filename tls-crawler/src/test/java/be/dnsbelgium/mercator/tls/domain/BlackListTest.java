package be.dnsbelgium.mercator.tls.domain;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class BlackListTest {

  private static final Logger logger = getLogger(BlackListTest.class);

  @Test
  public void add() throws UnknownHostException {
    BlackList blackList = new BlackList();
    String ipv6 = "2001:4860:4802:34::a";
    String ipv4 = "82.2.3.4";

    logger.info("Initial blackList: {}", blackList.blacklistAsString());

    assertThat(blackList.isBlacklisted(ipv4)).isFalse();
    assertThat(blackList.isBlacklisted(ipv6)).isFalse();

    blackList.add("2001:4860::/32");
    logger.info("BlackList after adding v6 range: {}", blackList.blacklistAsString());
    assertThat(blackList.isBlacklisted(ipv6)).isTrue();
    assertThat(blackList.isBlacklisted(ipv4)).isFalse();

    blackList.add("82.2.3.0/24");
    blackList.add("12.12.3.0/28");
    logger.info("BlackList after adding v4 range: {}", blackList.blacklistAsString());
    assertThat(blackList.isBlacklisted(ipv6)).isTrue();
    assertThat(blackList.isBlacklisted(ipv4)).isTrue();

    InetAddress inetAddressV4 = InetAddress.getByName(ipv4);
    InetAddress inetAddressV6 = InetAddress.getByName(ipv6);
    assertThat(blackList.isBlacklisted(inetAddressV4)).isTrue();
    assertThat(blackList.isBlacklisted(inetAddressV6)).isTrue();
  }

  @Test
  public void invalidIP() {
    BlackList blackList = new BlackList();
    // This is simply ignored and an error is logged.
    blackList.add("1.256.2.3");
    assertThat(blackList.isBlacklisted("1.256.2.3")).isFalse();
  }

  @Test
  public void ipv4Range() {
    BlackList blackList = new BlackList();
    blackList.add("1.2"); // corresponds to 1.0.0.2
    assertThat(blackList.isBlacklisted("1.2.2.2")).isFalse();
    assertThat(blackList.isBlacklisted("1.0.0.2")).isTrue();
  }

  // TODO add test or remove
  @Test
  public void npe() {
    BlackList blackList = new BlackList();

    InetSocketAddress address = new InetSocketAddress("quantumhypnosiscenter.be", 443);
    logger.info("address = {}", address);

    HostName hostName = new HostName(address);
    logger.info("hostName = {}", hostName);
    logger.info("hostName.isEmpty = {}", hostName.isEmpty());
    logger.info("address.getAddress() = {}", address.getAddress());

    //IPAddress ipAddress = new HostName(address).asAddress();

    if (blackList.isBlacklisted(address)) {
      logger.info("address isBlacklisted = {}", address);
    }

    //IPAddress address = new HostName(inetAddress).asAddress();

//    blackList.add("1.2");
//    blackList.isBlacklisted("1.2.3.x");
  }



}