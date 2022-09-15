package be.dnsbelgium.mercator.dns.domain.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;

import java.net.IDN;
import java.net.UnknownHostException;
import java.time.Duration;

public class DnsJavaUtil {

  private static final Logger logger = LoggerFactory.getLogger(DnsJavaUtil.class);

  public static Name getNameFromSubdomain(String domainName, String prefix) {
    Name apex = parseDomainName(domainName);
    if (apex == null) {
      return null;
    }

    Name fqdn = null;
    try {
      fqdn = Name.fromString(prefix, apex);
    } catch (TextParseException e) {
      logger.error("Something is wrong with the subdomain [{}]. Ignoring it.", prefix, e);
    }
    return fqdn;
  }

  private static Name parseDomainName(String domainName) {
    Name fqdn;
    String asciiDomainName;
    try {
      asciiDomainName = IDN.toASCII(domainName);
    } catch(IllegalArgumentException e) {
      logger.error("Cannot encode domain [{}] into ascii. Ignoring this visit request", domainName, e);
      return null;
    }
    try {
      fqdn = new Name(asciiDomainName, Name.root);
    } catch (TextParseException e) {
      logger.error("Cannot correctly parse domain name [{}] included in the visit request. Ignoring this visit request", domainName, e);
      return null;
    }
    return fqdn;
  }

  public static Resolver createResolver(String hostName, int port, int timeoutSeconds) {
    try {
      SimpleResolver simpleResolver = new SimpleResolver(hostName);
      simpleResolver.setPort(port);
      simpleResolver.setTimeout(Duration.ofSeconds(timeoutSeconds));
      return simpleResolver;
    } catch (UnknownHostException e) {
      logger.error("Failed to create SimpleResolver", e);
      throw new RuntimeException(e);
    }
  }

}
