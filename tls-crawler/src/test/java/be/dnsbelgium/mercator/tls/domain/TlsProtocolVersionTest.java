package be.dnsbelgium.mercator.tls.domain;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class TlsProtocolVersionTest {

  private static final Logger logger = getLogger(TlsProtocolVersionTest.class);

  @Test
  public void convertToStringAndBack() {
    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      String name = version.getName();
      logger.info("version.getName() = {}", name);
      TlsProtocolVersion back = TlsProtocolVersion.of(name);
      assertThat(back).isEqualTo(version);
    }
  }

  @Test
  public void nameAndBack() {
    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      String name = version.name();
      logger.info("name = {}", name);
      TlsProtocolVersion back = TlsProtocolVersion.of(name);
      assertThat(back).isEqualTo(version);
    }
  }

}