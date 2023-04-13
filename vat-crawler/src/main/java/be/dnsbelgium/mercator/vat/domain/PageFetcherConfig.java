package be.dnsbelgium.mercator.vat.domain;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.io.File;
import java.time.Duration;
import java.util.StringJoiner;

import static org.slf4j.LoggerFactory.getLogger;

@ConstructorBinding
@ConfigurationProperties("page-fetcher")
public class PageFetcherConfig {

  private final File cacheDirectory;
  private final DataSize cacheSize;
  private final Duration callTimeOut;
  private final Duration connectTimeOut;
  private final Duration readTimeOut;
  private final Duration writeTimeOut;
  private final Duration cacheMaxStale;
  private final String userAgent;
  private final DataSize maxContentLength;

  private final static String DEFAULT_USER_AGENT
      = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:87.0) Gecko/20100101 Firefox/87.0";

  private static final Logger logger = getLogger(PageFetcherConfig.class);

  public PageFetcherConfig(
      @DefaultValue("/tmp/cache/")  File cacheDirectory,
      @DefaultValue("100MB")        DataSize cacheSize,
      @DefaultValue("24h")          Duration cacheMaxStale,
      @DefaultValue("5s")           Duration connectTimeOut,
      @DefaultValue("5s")           Duration readTimeOut,
      @DefaultValue("5s")           Duration writeTimeOut,
      @DefaultValue("5s")           Duration callTimeOut,
      @DefaultValue("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:87.0) Gecko/20100101 Firefox/87.0")
                                    String userAgent,
      @DefaultValue("8MB")
                                    DataSize maxContentLength
      ) {
    this.cacheDirectory = cacheDirectory;
    this.cacheSize = cacheSize;
    this.cacheMaxStale = cacheMaxStale;
    logger.info("cacheSize = {} MB", cacheSize.toMegabytes());
    logger.info("cacheDirectory = {}", cacheDirectory);
    this.connectTimeOut = connectTimeOut;
    this.readTimeOut = readTimeOut;
    this.writeTimeOut = writeTimeOut;
    this.callTimeOut = callTimeOut;
    this.userAgent = userAgent;
    this.maxContentLength = maxContentLength;
  }

  public static PageFetcherConfig defaultConfig() {
    return new PageFetcherConfig(
        new File("/tmp/cache/"),
        DataSize.of(100, DataUnit.MEGABYTES),
        Duration.ofHours(24),
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        Duration.ofSeconds(8),
        DEFAULT_USER_AGENT,
        DataSize.of(8, DataUnit.MEGABYTES)
    );
  }

  public File getCacheDirectory() {
    return cacheDirectory;
  }

  public Duration getReadTimeOut() {
    return readTimeOut;
  }

  public Duration getConnectTimeOut() {
    return connectTimeOut;
  }

  public Duration getWriteTimeOut() {
    return writeTimeOut;
  }

  public DataSize getCacheSize() {
    return cacheSize;
  }

  public Duration getCacheMaxStale() {
    return cacheMaxStale;
  }

  public Duration getCallTimeOut() {
    return callTimeOut;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public DataSize getMaxContentLength() {
    return maxContentLength;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", PageFetcherConfig.class.getSimpleName() + "[", "]")
        .add("cacheDirectory=" + cacheDirectory)
        .add("cacheSize=" + cacheSize.toMegabytes() + "MB")
        .add("cacheMaxStale=" + cacheMaxStale)
        .add("connectTimeOut=" + connectTimeOut)
        .add("readTimeOut=" + readTimeOut)
        .add("writeTimeOut=" + writeTimeOut)
        .add("callTimeOut=" + callTimeOut)
        .add("maxContentLength=" + maxContentLength.toMegabytes() + "MB")
        .toString();
  }
}

