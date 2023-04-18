package be.dnsbelgium.mercator.vat.domain;

import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.io.File;
import java.time.Duration;

public class TestPageFetcherConfig {
  public static PageFetcherConfig testConfig() {
    return new PageFetcherConfig(
      new File("/tmp/cache/"),
      DataSize.of(100, DataUnit.MEGABYTES),
      Duration.ofHours(24),
      Duration.ofSeconds(5),
      Duration.ofSeconds(5),
      Duration.ofSeconds(5),
      Duration.ofSeconds(8),
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:87.0) Gecko/20100101 Firefox/87.0",
      DataSize.of(100, DataUnit.KILOBYTES)
    );
  }
}
