package be.dnsbelgium.mercator.tls.domain;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

import static org.slf4j.LoggerFactory.getLogger;

public class AddressTest {

  private static final Logger logger = getLogger(AddressTest.class);

  @Test
  public void address() {
    InetSocketAddress address = new InetSocketAddress("zzz.google.be", 443);
    boolean unresolved = address.isUnresolved();
    logger.info("unresolved = {}", unresolved);
    logger.info("address = {}", address);
    if (address.getAddress() != null) {
      logger.info("address = {}", address.getAddress().getHostAddress());
    }
  }

}
