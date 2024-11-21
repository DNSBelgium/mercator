package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.BlacklistEntry;
import be.dnsbelgium.mercator.tls.crawler.persistence.repositories.BlacklistEntryRepository;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.format.util.AddressTrie;
import inet.ipaddr.ipv4.IPv4AddressTrie;
import inet.ipaddr.ipv6.IPv6AddressTrie;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlackList {

  private static final IPv4AddressTrie blacklisted_v4 = new IPv4AddressTrie();
  private static final IPv6AddressTrie blacklisted_v6 = new IPv6AddressTrie();

  public BlackList() {
  }

  @Autowired
  public BlackList(BlacklistEntryRepository repository) {
    List<BlacklistEntry> entries = repository.findAll();
    for (BlacklistEntry entry : entries) {
      this.add(entry.getCidrPrefix());
    }
  }

  private static final Logger logger = getLogger(BlackList.class);

  public boolean isBlacklisted(String ip) {
    IPAddressString addressString = new IPAddressString(ip);
    return isBlacklisted(addressString.getAddress());
  }

  public boolean isBlacklisted(IPAddress ipAddress) {
    boolean blacklisted = false;
    if (ipAddress == null) {
      logger.error("BlackList.isBlacklisted() called with null argument => returning false");
      return false;
    }
    if (ipAddress.isIPv4()) {
      blacklisted = blacklisted_v4.elementContains(ipAddress.toIPv4());
      logger.debug("IPv4 address {} is blacklisted: {}", ipAddress, blacklisted);
    }
    if (ipAddress.isIPv6()) {
      blacklisted = blacklisted_v6.elementContains(ipAddress.toIPv6());
      logger.debug("IPv6 address {} is blacklisted: {}", ipAddress, blacklisted);
    }
    return blacklisted;
  }

  public boolean isBlacklisted(InetSocketAddress address) {
    if (address.isUnresolved()) {
      return false;
    }
    return isBlacklisted(address.getAddress());
  }

  public boolean isBlacklisted(InetAddress inetAddress) {
    IPAddress address = new HostName(inetAddress).asAddress();
    return isBlacklisted(address);
  }

  public void add(String cidr) {
    logger.debug("About to add {} to the blacklist", cidr);
    IPAddressString addressString = new IPAddressString(cidr);
    inet.ipaddr.IPAddress range = addressString.getAddress();
    if (range == null) {
      String msg = addressString.getAddressStringException().getMessage();
      logger.error("Could not add [{}] to the blacklist as it is not valid: {}", cidr, msg);
      return;
    }
    if (range.isIPv4()) {
      blacklisted_v4.add(range.toIPv4());
      logger.info("Added {} to the IPv4 blacklist. Blacklisted {} addresses", cidr, range.getCount());
    }
    if (range.isIPv6()) {
      blacklisted_v6.add(range.toIPv6());
      logger.info("Added {} to the IPv6 blacklist. Blacklisted {} addresses", cidr, range.getCount());
    }
  }

  public String blacklistAsString() {
    return AddressTrie.toString(false, blacklisted_v4, blacklisted_v6);
  }

}
