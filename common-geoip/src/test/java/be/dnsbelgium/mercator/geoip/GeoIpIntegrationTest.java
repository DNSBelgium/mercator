package be.dnsbelgium.mercator.geoip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SpringJUnitConfig(TestConfig.class)
@TestPropertySource(properties = "geo.ip.maxmind.license-key=test") // the key needs to be correct for the integration test to work
@Disabled
public class GeoIpIntegrationTest {

  private static final Logger logger = getLogger(GeoIpIntegrationTest.class);

  @Autowired GeoIPService geoIPService;

  @Test

  public void loadApplicationContext() {
    logger.info("geoIPService = {}", geoIPService);
    Optional<String> country = geoIPService.lookupCountry("8.8.8.8");
    logger.info("country = {}", country);
    assertThat(country.isPresent()).isTrue();
    assertThat(country.orElse("")).isEqualTo("US");
  }

}
