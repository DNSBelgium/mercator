package be.dnsbelgium.mercator.web.domain;

import be.dnsbelgium.mercator.web.domain.ConfigurableDns.SupportedIpVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class ConfigurableDnsTest {

  private static final Logger logger = getLogger(ConfigurableDnsTest.class);

  private static boolean avoidInternet() {
    return true;
  }

  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  @Test
  public void bothEnabled() throws UnknownHostException {
    ConfigurableDns configurableDns = new ConfigurableDns(SupportedIpVersion.BOTH);
    List<InetAddress> ips = configurableDns.lookup("www.dnsbelgium.be");
    logger.info("ips = {}", ips);
    boolean v4Found = ips.stream().anyMatch(inetAddress -> inetAddress instanceof Inet4Address);
    boolean v6Found = ips.stream().anyMatch(inetAddress -> inetAddress instanceof Inet6Address);
    assertThat(v4Found).isTrue();
    assertThat(v6Found).isTrue();
  }

  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  @Test
  public void v4Only() throws UnknownHostException {
    ConfigurableDns configurableDns = new ConfigurableDns(SupportedIpVersion.V4_ONLY);
    List<InetAddress> ips = configurableDns.lookup("www.dnsbelgium.be");
    logger.info("ips = {}", ips);
    boolean v4Found = ips.stream().anyMatch(inetAddress -> inetAddress instanceof Inet4Address);
    boolean v6Found = ips.stream().anyMatch(inetAddress -> inetAddress instanceof Inet6Address);
    assertThat(v4Found).isTrue();
    assertThat(v6Found).isFalse();
  }

  @Test
  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  public void v6Only() throws UnknownHostException {
    ConfigurableDns configurableDns = new ConfigurableDns(SupportedIpVersion.V6_ONLY);
    List<InetAddress> ips = configurableDns.lookup("www.dnsbelgium.be");
    logger.info("ips = {}", ips);
    boolean v4Found = ips.stream().anyMatch(inetAddress -> inetAddress instanceof Inet4Address);
    boolean v6Found = ips.stream().anyMatch(inetAddress -> inetAddress instanceof Inet6Address);
    assertThat(v4Found).isFalse();
    assertThat(v6Found).isTrue();
  }

}