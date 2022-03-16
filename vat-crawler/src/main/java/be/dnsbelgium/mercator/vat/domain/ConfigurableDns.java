package be.dnsbelgium.mercator.vat.domain;

import okhttp3.Dns;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class ConfigurableDns implements Dns {

  private static final Logger logger = getLogger(ConfigurableDns.class);

  private final SupportedIpVersion supportedIpVersion;

  // we will have to update this class for v7 and later ;-)
  public enum SupportedIpVersion {
    V4_ONLY,
    V6_ONLY,
    BOTH
  }

  public ConfigurableDns(SupportedIpVersion supportedIpVersion) {
    this.supportedIpVersion = supportedIpVersion;
  }

  @NotNull
  @Override
  public List<InetAddress> lookup(@NotNull String s) throws UnknownHostException {
    List<InetAddress> addresses = SYSTEM.lookup(s);
    List<InetAddress> result = new ArrayList<>();
    for (InetAddress inetAddress : addresses) {
      if (inetAddress instanceof Inet4Address) {
        if (supportedIpVersion != SupportedIpVersion.V6_ONLY) {
          result.add(inetAddress);
        } else {
          logger.debug("{} => removing {} for {}", supportedIpVersion, inetAddress, s);
        }
      }
      if (inetAddress instanceof Inet6Address) {
        if (supportedIpVersion != SupportedIpVersion.V4_ONLY) {
          result.add(inetAddress);
        } else {
          logger.debug("{} => removing {} for {}", supportedIpVersion, inetAddress, s);
        }
      }
    }
    return result;
  }
}
