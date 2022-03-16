package be.dnsbelgium.mercator.geoip;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MaxMindConfigTest {

  @Test
  public void paid() {
    MaxMindConfig config = MaxMindConfig.paid(Duration.ofDays(1), "key", "loc");
    assertThat(config.getFileLocation()).isEqualTo("loc");
    assertThat(config.isUsePaidVersion()).isTrue();
    assertThat(config.getLicenseKey()).isEqualTo("key");
  }

  @Test
  public void free() {
    MaxMindConfig config = MaxMindConfig.free(Duration.ofHours(50), "key", "loc");
    assertThat(config.getFileLocation()).isEqualTo("loc");
    assertThat(config.isUsePaidVersion()).isFalse();
    assertThat(config.getLicenseKey()).isEqualTo("key");
    assertThat(config.getMaxFileAgeInDays()).isEqualTo(2);
    assertThat(config.getMaxFileAge()).isEqualTo(Duration.ofHours(50));
  }

  @Test
  public void locationIsNull() {
    MaxMindConfig config = MaxMindConfig.free(Duration.ofHours(50), "key", null);
    String expected = System.getProperty("java.io.tmpdir") + "/maxmind";
    assertThat(config.getFileLocation()).isEqualTo(expected);
    assertThat(config.isUsePaidVersion()).isFalse();
    assertThat(config.getLicenseKey()).isEqualTo("key");
    assertThat(config.getMaxFileAgeInDays()).isEqualTo(2);
    assertThat(config.getMaxFileAge()).isEqualTo(Duration.ofHours(50));
  }

}