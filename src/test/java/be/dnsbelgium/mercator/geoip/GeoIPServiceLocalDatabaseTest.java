package be.dnsbelgium.mercator.geoip;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoIPServiceLocalDatabaseTest {

  @TempDir
  private File maxmindFolder;

  @Test
  void autoUpdateStillRequiresLicenseKey() {
    MaxMindConfig config = MaxMindConfig.free(
        Duration.ofDays(1), null, maxmindFolder.getAbsolutePath());

    assertThatThrownBy(() -> new GeoIPServiceImpl(config))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("No valid Maxmind license key");
  }

  @Test
  void missingCountryDatabaseFailsWithoutLicenseKey() {
    MaxMindConfig config = localConfig(null);

    assertThatThrownBy(() -> new GeoIPServiceImpl(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GeoLite2-Country.mmdb")
        .hasMessageContaining("auto-update is disabled");
  }

  @Test
  void missingAsnDatabaseFailsClearly() throws IOException {
    assertThat(new File(maxmindFolder, "GeoLite2-Country.mmdb").createNewFile()).isTrue();

    assertThatThrownBy(() -> new GeoIPServiceImpl(localConfig(null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GeoLite2-ASN.mmdb")
        .hasMessageContaining("auto-update is disabled");
  }

  @Test
  void localDatabaseModeDoesNotMakeHttpRequests() throws IOException {
    assertThat(new File(maxmindFolder, "GeoLite2-Country.mmdb").createNewFile()).isTrue();
    assertThat(new File(maxmindFolder, "GeoLite2-ASN.mmdb").createNewFile()).isTrue();

    try (MockWebServer server = new MockWebServer()) {
      server.start();

      assertThatThrownBy(() -> new GeoIPServiceImpl(localConfig(server.url("/").toString())))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Error initializing Maxmind GEO/ASN database");
      assertThat(server.getRequestCount()).isZero();
    }
  }

  private MaxMindConfig localConfig(String url) {
    String databaseUrl = url == null ? MaxMindConfig.DEFAULT_URL_FREE_ASN_DB : url;
    return new MaxMindConfig(
        Duration.ofDays(1),
        databaseUrl,
        databaseUrl,
        false,
        false,
        null,
        maxmindFolder.getAbsolutePath());
  }
}
