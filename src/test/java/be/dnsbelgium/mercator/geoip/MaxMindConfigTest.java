package be.dnsbelgium.mercator.geoip;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class MaxMindConfigTest {

  @Test
  void autoUpdateIsEnabledByDefault() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("geo.ip.maxmind.file-location", "/tmp/maxmind");

    MaxMindConfig config = bind(environment);

    assertThat(config.isAutoUpdate()).isTrue();
  }

  @Test
  void autoUpdateCanBeDisabled() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("geo.ip.maxmind.auto-update", "false");

    MaxMindConfig config = bind(environment);

    assertThat(config.isAutoUpdate()).isFalse();
  }

  private MaxMindConfig bind(MockEnvironment environment) {
    return Binder.get(environment)
        .bind("geo.ip.maxmind", MaxMindConfig.class)
        .orElseThrow(() -> new AssertionError("MaxMind configuration was not bound"));
  }
}
